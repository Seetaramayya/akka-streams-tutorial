package tutorial.akka.streams.actor.interop

import akka.actor.{ ActorSystem, Props }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.util.Timeout
import scala.concurrent.duration._

object ActorAsFlow extends App {
  implicit val system = ActorSystem("integrate-with-actors")
  implicit val timeout = Timeout(1.second)
  val simpleActorRef = system.actorOf(Props(new SimpleActor))
  import system.dispatcher
  // Actor as a flow
  val flow = Flow[Int].ask[Int](5)(simpleActorRef)

  // ask source
  //  Source(1 to 10).ask[Int](5)(simpleActorRef).runWith(Sink.foreach(println)).onComplete { _ => system.terminate() }

  // ask flow
  Source(1 to 10).via(flow).runWith(Sink.foreach(println)).onComplete { _ => system.terminate() }
}
