package tutorial.akka.streams.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ ClosedShape, SinkShape, SourceShape }
import akka.stream.scaladsl.{ Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source }

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

/**
 * A composite component
 * - print all components which are lower case
 * - count strings that are short ( _.length < 5)
 */
object GraphMaterializedValuesExample1 extends App {
  implicit val system = ActorSystem("GraphMaterializedValues")

  private val strings = List("Akka", "is", "awesome", "scala", "rocks")
  val source = Source(strings)

  val printer = Sink.foreach(println)
  val counter = Sink.fold[Int, String](0)((counter, _) => counter + 1)

  //Step1: Preparation
  val sinkShape = Sink.fromGraph {
    GraphDSL.create(counter) { implicit builder => counterShape =>
      //Step2: Bring necessary components to scope
      import GraphDSL.Implicits._

      //Step3: Add necessary components
      val broadcast = builder.add(Broadcast[String](2))
      val shortFilter = builder.add(Flow[String].filter(_.length < 5))
      val lowercaseFilter = builder.add(Flow[String].filter(str => str == str.toLowerCase))

      //Step4: Connect components
      // format: OFF
      broadcast ~> shortFilter ~> counterShape
      broadcast ~> lowercaseFilter ~> printer
      // format: ON

      //Step5: Return shape
      SinkShape(broadcast.in)
    }
  }
  import system.dispatcher
  source.runWith(sinkShape).onComplete {
    case Success(count)     => println(s"Number of short strings are $count")
    case Failure(exception) => println(s"Something went wrong while calculating short strings $exception")
  }
}
