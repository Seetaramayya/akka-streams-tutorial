package tutorial.akka.streams.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip }

import scala.concurrent.duration._

/**
 * Feed a source into 2 sinks at the same time
 *
 * {{{
 *     +--------------------------------------------------------------------------------+
 *     |                                                                                |
 *     |  +------------+        +------------+        +------------+        +--------+  |
 *     |  | fast source| ~Out~> |            |        |            | ~Out~> | sink 1 |  |
 *     |  +------------+        |            |        |            |        +--------+  |
 *     |                        |   Merge    | ~Out~> |   Balance  |                    |
 *     |  +------------+        |            |        |            |        +--------+  |
 *     |  | slow source| ~Out~> |            |        |            | ~Out~> | sink 2 |  |
 *     |  +------------+        +------------+        +------------+        +--------+  |
 *     |                                                                                |
 *     +--------------------------------------------------------------------------------+
 * }}}
 */
object ClosedGraphExample2 extends App {
  implicit val system = ActorSystem("BitInvolvedGraph")
  val fastSource = Source(1 to 10).map(x => s"source1 $x")
  val slowSource = Source(1 to 10).map(x => s"source2 $x").throttle(1, 1.second)

  val sink1 = Sink.foreach[String](output => println(s"Sink1 $output"))
  val sink2 = Sink.foreach[String](output => println(s" Sink2 $output"))

  //Step1: Preparation
  val graph = RunnableGraph.fromGraph {
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      //Step2: Bring necessary components to scope
      import GraphDSL.Implicits._

      //Step3: Add necessary components
      val merge = builder.add(Merge[String](2))
      val balance = builder.add(Balance[String](2))

      //Step4: Connect components
      // format: OFF
      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge
                             balance ~> sink2
      // format: ON

      //Step5: Return shape
      ClosedShape
    }

  }
  graph.run()
}
