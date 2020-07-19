package tutorial.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }

import scala.concurrent.duration._

object HelloStream extends App {
  implicit private val system: ActorSystem = ActorSystem()

  def repeatMessage(message: String = "Hello World!"): Source[String, NotUsed] = Source.repeat(message)

  repeatMessage().throttle(1, 500.millis).to(Sink.foreach(println)).run()
}
