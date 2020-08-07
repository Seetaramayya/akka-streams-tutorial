package tutorial.akka.streams.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ FlowShape, SinkShape }
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Keep, Merge, Sink, Source }

import scala.concurrent.Future
import scala.util.Success

/**
 * Concat drains first source and then second source
 * {{{
 *     +------------------------------------------------------+
 *     |                                                      |
 *     |  +------------+        +---------+       +--------+  |
 *     |  |          ~Out~>  ~IN~>  flow1 |  ~~~> |        |  |
 *     |  |            |        +---------+       |        |  |
 *   ~IN~>| Broadcast  |                          | Merge  | ~OUT~>
 *     |  |            |        +---------+       |        |  |
 *     |  |          ~Out~>  ~IN~>  flow2 |  ~~~> |        |  |
 *     |  +------------+        +---------+       +--------+  |
 *     |                                                      |
 *     +------------------------------------------------------+
 * }}}
 */
object FlowGraphExample1 extends App {
  implicit val system = ActorSystem("SourceGraph")
  implicit val ec = system.dispatcher
  val source = Source(1 to 10)
  val flow1 = Flow[Int].map(_ + 1)
  val flow2 = Flow[Int].map(_ * 10)

  val flowGraph = Flow.fromGraph {
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))
      broadcast ~> flow1 ~> merge
      broadcast ~> flow2 ~> merge
      FlowShape[Int, Int](broadcast.in, merge.out)
    }
  }

  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val sink = Sink.fold[Int, A](0)((count, _) => count + 1)
    Flow.fromGraph {
      GraphDSL.create(sink) { implicit builder => sinkShape =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[A](2))
        val flowShape = builder.add(flow)
        broadcast.out(0) ~> flowShape
        broadcast.out(1) ~> sinkShape

        FlowShape(broadcast.in, flowShape.out)
      }
    }
  }

//  source.via(flowGraph).runWith(Sink.foreach(println))
  val simpleFlow = Flow[Int].map(identity)
  val futureCount = source.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(Sink.ignore)(Keep.left).run()
  futureCount.onComplete {
    case Success(count) => println(s"coutn is $count")
    case _              => println("error")
  }
}
