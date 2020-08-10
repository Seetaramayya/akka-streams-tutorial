package tutorial.akka.streams.actor.interop

import akka.actor.{ ActorSystem, Props }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import SinkActor._

import scala.concurrent.duration._

object ActorAsSink extends App {
  implicit val system = ActorSystem("integrate-with-actors-as-sink")
  implicit val timeout = Timeout(1.second)
  // Actor as a sink
  val sinkActor = system.actorOf(Props(new SinkActor), "sink-actor")
  val actorRefSink = Sink.actorRefWithBackpressure[Int](sinkActor, StreamInit, StreamAck, StreamClose, error => StreamFailure(error))

  Source(1 to 10).runWith(actorRefSink)

}
