package tutorial.akka.streams.graph

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.javadsl.MergePreferred
import akka.stream.scaladsl.{ Balance, Flow, GraphDSL, RunnableGraph, Source, ZipLatestWith, ZipWith }

/**
 * Observe even though source has 1000 elements only 1 or 2 elements will be taken rest is coming from the outlet of flow
 * {{{
 *     +-------------------------------------------------------------------------+
 *     |                                                                         |
 *     |  +------------+        +------------+        +------------+             |
 *     |  | source     | ~Out~> |            |        |            |             |
 *     |  +------------+        |   ZipWith  |        |            |             |
 *     |                        |  Preferred | ~Out~> |   Flow     |~~~~Out~~~+  |
 *     |                        |            |        |            |          v  |
 *     |             >>>>>Out~> |(Pref Inlet)|        |            |          v  |
 *     |             ^          +------------+        +------------+          v  |
 *     |             ^<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  |
 *     +-------------------------------------------------------------------------+
 * }}}
 */
object GraphCyclesExample2 extends App {
  implicit val system = ActorSystem()
  val firstSource = Source.repeat[Int](1)
  val secondSource = Source.repeat[Int](1)
  val zipWithGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val firstSourceShape = builder.add(firstSource)
    val balanceShape = builder.add(Balance[Int](2))
    val mergePreferredShape = builder.add(MergePreferred.create[Int](1))
    val repeater = builder.add(Flow[Int].map { i =>
      println(s"repeater from the merge, repeatingn $i")
      i
    })
    val zipWithShape = builder.add(ZipWith[Int, Int, Int] { (in1, in2) =>
      Thread.sleep(100)
      println(s"($in1, $in2) => ${in1 + in2}")
      in1 + in2
    })
    // Format: OFF
    firstSourceShape ~> balanceShape ~> mergePreferredShape ~> repeater ~> zipWithShape.in0
                                              mergePreferredShape <~ zipWithShape.out
                              balanceShape ~> zipWithShape.in1
    // Format: ON
    ClosedShape
  }

  RunnableGraph.fromGraph(zipWithGraph).run()
}
