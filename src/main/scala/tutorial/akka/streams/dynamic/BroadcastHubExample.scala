package tutorial.akka.streams.dynamic

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.Attributes
import akka.stream.scaladsl.{ BroadcastHub, Sink, Source }

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Dynamic stream handling:
 *  https://doc.akka.io/docs/akka/current/stream/stream-dynamic.html
 */
object BroadcastHubExample extends App {
  implicit val system = ActorSystem("dynamic-stream-handling")
  import system.dispatcher

  val broadCastHubSink = BroadcastHub.sink[Int](bufferSize = 256)
  val materializedSource: Source[Int, NotUsed] = Source(1 to 10)
    .throttle(10, 1.second)
    .log("broad-cast-hub")
    .runWith(broadCastHubSink)

  val broadCastedCollector: Future[Seq[Int]] = materializedSource.runWith(Sink.seq[Int])
  val broadCastedPrinter: Future[Done] = materializedSource
    .log("printer")
    .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel, onFailure = Logging.ErrorLevel, onFinish = Logging.InfoLevel))
    .runWith(Sink.ignore)
  val broadCastedCounter: Future[Int] = materializedSource
    .runWith(Sink.fold[Int, Int](0) {
      case (counter, elem) => counter + 1
    })

  for {
    elements <- broadCastedCollector
    numberOfElements <- broadCastedCounter
  } yield {
    println(elements)
    println(numberOfElements)
    println(s"Are the elements same :) ? ${elements.size == numberOfElements}")
  }

}
