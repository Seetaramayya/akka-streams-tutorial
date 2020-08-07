package tutorial.akka.streams.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip }

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  val input = Source(1 to 10)
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)
  val output = Sink.foreach[(Int, Int)](println)

  // Step1: Necessary preparations for Graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // Builder is mutable until shape
      // Step2: Bring implicit operators into scope
      import GraphDSL.Implicits._

      // Step3: add necessary components to the graph
      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, Int]())

      // Step4: prepare necessary connections
      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // Step5: return shape
      ClosedShape // Builder is not mutable after this
    } // graph
  ) // runnable graph that can run

  graph.run()
  system.terminate()
}
