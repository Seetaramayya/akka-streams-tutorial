package tutorial.akka.streams.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ SinkShape, SourceShape }
import akka.stream.scaladsl.{ Broadcast, Concat, GraphDSL, Sink, Source }

import scala.concurrent.duration._

/**
 * Concat drains first source and then second source
 * {{{
 *     +------------------------------------+
 *     |                                    |
 *     |  +------------+        +-------+   |
 *     |  |            | ~Out~> | sink1 |   |
 *     |  |            |        +-------+   |
 *   ~IN~>| Broadcast  |                    |
 *     |  |            |        +-------+   |
 *     |  |            | ~Out~> | sink2 |   |
 *     |  +------------+        +-------+   |
 *     |                                    |
 *     +------------------------------------+
 * }}}
 */
object SinkGraphExample1 extends App {
  implicit val system = ActorSystem("SourceGraph")
  implicit val ec = system.dispatcher
  val sink1 = Sink.foreach[Int](x => println(s"Sink1 $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Sink2 $x"))

  val sinkGraph: Sink[Int, NotUsed] = Sink.fromGraph {
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      broadcast ~> sink1
      broadcast ~> sink2
      SinkShape[Int](broadcast.in)
    }
  }

  Source(1 to 10).runWith(sinkGraph)
}
