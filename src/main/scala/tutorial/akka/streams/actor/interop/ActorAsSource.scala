package tutorial.akka.streams.actor.interop

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.{ CompletionStrategy, OverflowStrategy }
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object ActorAsSource extends App {
  implicit val system = ActorSystem("integrate-with-actors-as-source")
  implicit val timeout = Timeout(1.second)
  import system.dispatcher
  // Actor as a source
  val actorRefSource = Source.actorRef[Int](10, OverflowStrategy.fail)
  val (steamActorRef, totalElementsInSinkFuture) = actorRefSource.toMat(Sink.seq[Int])(Keep.both).run()
  (1 to 100).foreach(i => steamActorRef ! i)
  steamActorRef ! akka.actor.Status.Success(CompletionStrategy.immediately)

  totalElementsInSinkFuture.onComplete {
    case Success(values) =>
      println(s"Expecting to fail, but sink received ${values.size}")
      system.terminate()
    case Failure(ex) =>
      println(ex.getMessage)
      system.terminate()
  }

}
