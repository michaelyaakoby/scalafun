package io.github.michaelyaakoby.scalafun.fscanner

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.Await
import scala.concurrent.duration._

object ScannerMain extends App {

  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val st = System.currentTimeMillis()

  val dirScanner = new DirScannerSource(Paths.get("/tmp"))

  val scanF = dirScanner
    .scanSource()
    .groupedWithin(10000, 10.seconds)
    .scan((0, 0L, System.currentTimeMillis())) { (acc, paths) =>
      val now = System.currentTimeMillis()
      (acc._1 + paths.length, now - acc._3, now)
    }
    .runWith(Sink.foreach {case (entries, time, _) =>
      println(s"$entries new entries in $time millis")
    })
    .map(_ => dirScanner.cleanup())

  Await.result(scanF, 10.minutes)

  println(s"Time: ${(System.currentTimeMillis() - st) / 1000}")

  mat.shutdown()
  sys.terminate()
}
