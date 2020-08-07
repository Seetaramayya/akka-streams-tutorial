package tutorial.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ FlowShape, SinkShape }
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Sink, Source }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object CheatSheet extends App {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  // Source.single
//  val f1 = Source.single(1).runWith(Sink.foreach(result => println(s"Source single emitted value: $result")))

  // Source.apply(Iterable)
//  val f2 = Source(List(1, 2, 3)).runWith(Sink.foreach(result => println(s"Source iterable emitted value: $result")))
//  val f3 = Source(1 to 10).runWith(Sink.foreach(result => println(s"Source iterable emitted value: $result")))

  // infinite stream: Source.fromIterator(() => Iterator.from(1))
  val f4 = Source(LazyList.from(1))
    .throttle(1, 1.second)
//    .runWith(Sink.foreach(result => println(s"Source with infinite elements: $result")))

  // simple word count
  val sentences = List("Akka is awesome", "I love scala", "I am learning akka streams", "Streams are not easy as of now")
  val sentencesSource = Source(sentences)
  val sentenceToWord = Flow[String].map(_.split("""\s"""))
  val wordCount = Flow[Array[String]].map(_.length)

  val f5 = sentencesSource
    .via(sentenceToWord)
    .via(wordCount)
    .fold(0)(_ + _)
//    .runWith(Sink.foreach(result => println(s"Word count is $result")))

  Source(1 to 3)
    .map { element => println(s"FlowA $element"); element }
    .async
    .map { element => println(s"FlowB $element"); element }
    .async
    .map { element => println(s"FlowC $element"); element }
    .async
    .runWith(Sink.ignore)

}
