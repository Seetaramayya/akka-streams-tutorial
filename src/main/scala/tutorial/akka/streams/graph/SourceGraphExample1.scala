package tutorial.akka.streams.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.SourceShape
import akka.stream.scaladsl.{ Concat, GraphDSL, Keep, Sink, Source }

import scala.concurrent.duration._

/**
 * Concat drains first source and then second source
 * {{{
 *     +-----------------------------------------+
 *     |                                         |
 *     |  +------------+        +------------+   |
 *     |  | fast source| ~Out~> |            |   |
 *     |  +------------+        |            |   |
 *     |                        |   Concat   | ~Out~>
 *     |  +------------+        |            |   |
 *     |  | slow source| ~Out~> |            |   |
 *     |  +------------+        +------------+   |
 *     |                                         |
 *     +-----------------------------------------+
 * }}}
 */
object SourceGraphExample1 extends App {
  implicit val system = ActorSystem("SourceGraph")
  implicit val ec = system.dispatcher
  val fastSource = Source(1 to 10)
  val slowSource = Source(1 to 10).throttle(2, 1.second)

  val sourceGraph = Source.fromGraph {
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val concat = builder.add(Concat[Int]())
      fastSource ~> concat
      slowSource ~> concat
      SourceShape(concat.out)
    }
  }

  sourceGraph.runWith(Sink.foreach(println)).onComplete(_ => system.terminate())
}
