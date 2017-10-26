package io.github.michaelyaakoby.scalafun.fscanner

import java.nio.file.{Path, Paths}

import com.bluejeans.common.bigqueue.BigQueue
import com.google.common.util.concurrent.MoreExecutors
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

import better.files._

class PendingDirsQueue(offHeapDir: Path = Paths.get("target/fscanner"))(implicit ec: ExecutionContext) extends StrictLogging {

  private val offHeapDirFile: File = offHeapDir
  if (offHeapDirFile.exists) offHeapDirFile.delete()

  private val bigQueue = new BigQueue(offHeapDirFile.parent.toString(), offHeapDirFile.name)

  def size()  = bigQueue.size()
  def clear() = bigQueue.removeAll()

  def enqueue(entry: Path) = bigQueue.enqueue(toBytes(entry))

  def dequeue(retries: Int = 2): Future[Path] = {
    val promise       = Promise[Array[Byte]]()

    val dequeueFuture = bigQueue.dequeueAsync()
    dequeueFuture.addListener(() => promise.complete(Try(dequeueFuture.get())), MoreExecutors.directExecutor())

    promise.future
      .map(toEntry)
      .recoverWith{
        case _: NullPointerException if retries > 0 => dequeue(retries - 1)
      }
  }

  def close() = {
    logger.info(s"Closing and cleaning up the pending uploads queue")
    bigQueue.close()
    offHeapDirFile.delete(true)
  }

  def toBytes(entry: Path) = entry.toString.getBytes

  def toEntry(bytes: Array[Byte]) = Paths.get(new String(bytes))
}
