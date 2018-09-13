package io.github.michaelyaakoby.scalafun.fscanner

import java.nio.file._
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.Source
import akka.stream.stage._
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DirScannerSource(root: Path, offHeapDir: Path, concurrentScanners: Int = 64)(implicit ec: ExecutionContext) extends StrictLogging {

  private val queue = new PendingDirsQueue(offHeapDir)
  queue.enqueue(root)
  private val pendingOrIteratingDirs = new AtomicInteger(1)

  private val doneMarker = ".."

  def scanSource(): Source[Path, NotUsed] = {
    Source
      .repeat("tick")
      .mapAsyncUnordered(1)(dequeueNextDir)
      .takeWhile(_.exists(_.toString != doneMarker))
      .filter(_.isDefined).map(_.get)
      .flatMapMerge(concurrentScanners, childrenSource)
  }

  private def dequeueNextDir(dontCare: Any) =
    queue.dequeue().map(Some(_)).recover { case _: CancellationException => None }

  private def childrenSource(dirPath: Path) = new GraphStage[SourceShape[Path]] {
    case class DirIterator(stream: DirectoryStream[Path]) {
      val iter = stream.iterator()
    }

    val out: Outlet[Path]        = Outlet(s"children of $dirPath")
    val shape: SourceShape[Path] = SourceShape(out)

    def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

      var dirIterTry: Try[DirIterator] = _

      val pushCallback    = getAsyncCallback[Path](push(out, _)).invoke _
      val failureCallback = getAsyncCallback[Throwable](failStage).invoke _

      override def postStop(): Unit = if (dirIterTry != null) dirIterTry.map(_.stream.close())

      def closeAndFail(t: Throwable) = {
        logger.warn(s"Scanning of $dirPath failed", t)
        failureCallback(t)
        throw t
      }

      def tryOpen() =  {
        if (dirIterTry != null) dirIterTry
        else {
          dirIterTry = Try(DirIterator(java.nio.file.Files.newDirectoryStream(dirPath)))
          dirIterTry.recover {
            case t: Throwable => closeAndFail(t)
          }
        }
      }

      def tryHasNext(dirIter: DirIterator) =
        Try(dirIter.iter.hasNext)
          .map(_ -> dirIter)
          .recover { case t: Throwable => closeAndFail(t) }

      def tryNext(params: (Boolean, DirIterator)) = params match {
        case (hasNext, dirIter) =>
          if (hasNext) {
            Try(dirIter.iter.next())
              .map { entry =>
                if (Files.isDirectory(entry)) {
                  pendingOrIteratingDirs.incrementAndGet()
                  queue.enqueue(entry)
                }
                pushCallback(entry)
              }
              .recover { case t => closeAndFail(t) }
          } else {
            completeStage()

            if (pendingOrIteratingDirs.decrementAndGet() == 0) {
              logger.info(s"Done scanning $root - cleaning up scan")
              queue.enqueue(Paths.get(doneMarker))
            }
          }
      }

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit =
            Future.fromTry(tryOpen().flatMap(tryHasNext).map(tryNext)).recover { case t: Throwable => failStage(t) }
        }
      )
    }
  }

  def cleanup() = queue.close()

}
