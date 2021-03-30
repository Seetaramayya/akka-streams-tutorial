package tutorial.akka.streams.recap

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }

object Playground extends App {
  implicit val system = ActorSystem("recap-streams")

  val simpleSource = Source(1 to 10)
  val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)
  simpleSource.runWith(simpleSink)
}
