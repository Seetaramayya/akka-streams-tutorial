package tutorial.akka.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source

import scala.concurrent.Future

object Exercises extends App {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher
  private val names = List("Seeta", "Jack", "Jill", "Joe", "SeetaRamayya", "Vadali")

  // print first 2 names whose length is > 5
  val f1 = Source(names).filter(_.length > 5).take(2).runForeach(println)

  // after all futures shutdown system
  Future.sequence(List(f1)).onComplete(_ => system.terminate())
}
