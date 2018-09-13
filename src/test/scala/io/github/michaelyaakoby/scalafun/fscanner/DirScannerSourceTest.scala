package io.github.michaelyaakoby.scalafun.fscanner

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import better.files._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class DirScannerSourceTest extends WordSpec with BeforeAndAfter with Matchers {

  implicit var sys: ActorSystem = _
  implicit var mat: ActorMaterializer = _

  val enough = 35.seconds
  val defaultRoot = "target" / "scanner"
  var dirScanner: DirScannerSource = _

  before {
    sys = ActorSystem()
    mat = ActorMaterializer()
  }

  after {
    mat.shutdown()
    sys.terminate()

    dirScanner.cleanup()

    defaultRoot.delete(true)
  }

  def scan(root: File = defaultRoot) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val rootPath = root.path
    dirScanner = new DirScannerSource(
      rootPath,
      ("target" / "tmp").path
    )

    val scanF = dirScanner
      .scanSource()
      .map(toCanonical(root))
      .runFold(List.empty[String])((acc, res) => acc :+ res)
    Await.result(scanF, enough).sorted
  }

  def toCanonical(root: File)(path: Path) =
    root.relativize(path).toString.replaceAll("\\\\", "/")

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
  }

  "dir scanner" should {
    "sanity using a small tree" in {
      val a = (defaultRoot / "a").createDirectories()
      (a / "1").write("1")
      (a / "2").write("12")

      val b = (a / "b").createDirectories()
      (b / "3").write("123")
      (b / "4").write("1244")

      scan() should be(List("a", "a/1", "a/2", "a/b", "a/b/3", "a/b/4"))
    }

    "scan the entire project (~400 files and 400 dirs)" in {
      val root = File("./")
      val expectedPaths = time(root.listRecursively).toList.map(_.path).map(toCanonical(root)).filterNot(_.startsWith("target/tmp")).sorted
      val scannedPaths = time(scan(root)).filterNot(_.startsWith("target/tmp"))

      scannedPaths.size should be(expectedPaths.size)
    }
  }

}
