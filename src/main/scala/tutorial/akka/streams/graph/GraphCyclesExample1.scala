package tutorial.akka.streams.graph

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.javadsl.MergePreferred
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Source, ZipWith }

/**
 * Observe even though source has 1000 elements only 1 or 2 elements will be taken rest is coming from the outlet of flow
 * cycles risk deadlocking, this can be handled with add bounds to the number of elements in the cycles
 *
 * {{{
 *     +--------------------------------------------------------------------+
 *     |                                                                    |
 *     |  +-------+        +------------+        +------------+             |
 *     |  | source| ~Out~> |            |        |            |             |
 *     |  +-------+        |   Merge    |        |            |             |
 *     |                   |  Preferred | ~Out~> |   Flow     |~~~~Out~~~+  |
 *     |                   |            |        |            |          v  |
 *     |        >>>>>Out~> |(Pref Inlet)|        |            |          v  |
 *     |        ^          +------------+        +------------+          v  |
 *     |        ^<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  |
 *     +--------------------------------------------------------------------+
 * }}}
 */
object GraphCyclesExample1 extends App {
  implicit val system = ActorSystem()
  val source = Source[Int](1 to 1000)
  val mergePreferredCycle = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val sourceShape = builder.add(source)
    val mergeShape = builder.add(MergePreferred.create[Int](1))
    val flowShape = builder.add(Flow[Int].map { x =>
      println(s"flowing $x")
      x
    })
    sourceShape ~> mergeShape ~> flowShape
    mergeShape.preferred <~ flowShape.out

    ClosedShape
  }

  RunnableGraph.fromGraph(mergePreferredCycle).run()
}
