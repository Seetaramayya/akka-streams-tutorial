package tutorial.akka.streams

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ FileIO, Flow, Keep, Sink, Source }

import scala.io.{ Source => IOSource }
import scala.util.{ Failure, Success, Try }

object WordCount extends App {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  private val fileName = "sample.txt"
  private val uri = getClass.getClassLoader.getResource(fileName).toURI
  val path = Paths.get(uri)

  val wordsInLine = Flow[String].map(_.split("""\s+"""))
  val wordsCountInLine = Flow[Array[String]].map(_.length)
  val reduceSink = Sink.reduce[Int](_ + _)

  val fileIOSource = FileIO.fromPath(path).map(_.toString())
  val totalWords = FileIO
    .fromPath(path)
    .map(_.toString())
    .map(_.split("""\s+"""))
    .map(_.length)
    .runWith(Sink.reduce[Int](_ + _))

  fileIOSource.via(wordsInLine).via(wordsCountInLine).toMat(reduceSink)(Keep.right).run()

  val (source, lines) = Try(IOSource.fromFile(uri)).map(source => (source, source.getLines().toList)).get
  source.close()
  println(s"Number of lines are ${lines.size}")
  val sourceLines = Source(lines)

  totalWords.onComplete {
    case Success(count) =>
      println(s"Total words are $count")
      system.terminate()
    case Failure(exception) =>
      println(s"Failed while counting words $exception")
      system.terminate()
  }

}
