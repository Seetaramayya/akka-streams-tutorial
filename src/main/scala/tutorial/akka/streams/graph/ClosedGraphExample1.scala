package tutorial.akka.streams.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip }

/**
 * Feed a source into 2 sinks at the same time
 *
 * {{{
 *     +---------------------------------+
 *     |                                 |
 *     |  +--------+           +-------+ |
 *     |  |        |  ~~Out~~> | sink1 | |
 *     |  | source |           +-------+ |
 *     |  |        |           +-------+ |
 *     |  |        |  ~~Out~~> | sink2 | |
 *     |  +--------+           +-------+ |
 *     +---------------------------------+
 * }}}
 */
object ClosedGraphExample1 extends App {
  implicit val system = ActorSystem("GraphBasics")
  val input = Source(1 to 10)
  val sink1 = Sink.foreach[Int](x => println(s"Sink1 ${x + 1}"))
  val sink2 = Sink.foreach[Int](x => println(s"Sink2 ${x * 2}"))

  val graph = RunnableGraph.fromGraph {
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      input ~> broadcast
      broadcast.out(0) ~> sink1
      broadcast.out(1) ~> sink2
      ClosedShape
    }
  }

  graph.run()
  system.terminate()
}
