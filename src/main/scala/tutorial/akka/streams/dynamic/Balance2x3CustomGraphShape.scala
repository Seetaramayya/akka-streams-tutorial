package tutorial.akka.streams.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Balance, GraphDSL, Merge, RunnableGraph, Sink, Source }
import akka.stream._

import scala.concurrent.duration._

/**
 *
 * https://doc.akka.io/docs/akka/current/stream/stream-graphs.html#building-reusable-graph-components
 */
object Balance2x3CustomGraphShape extends App {
  implicit val system = ActorSystem("custom-graph-shape")

  case class Balance2x3[T](in0: Inlet[T], in1: Inlet[T], out0: Outlet[T], out1: Outlet[T], out2: Outlet[T]) extends Shape {
    override def inlets: Seq[Inlet[T]] = List(in0, in1)
    override def outlets: Seq[Outlet[T]] = List(out0, out1, out2)
    override def deepCopy(): Shape = Balance2x3(in0.carbonCopy(), in1.carbonCopy(), out0.carbonCopy(), out1.carbonCopy(), out2.carbonCopy())
  }

  object Balance2x3 {
    def apply[T](): Graph[Balance2x3[T], NotUsed] = {
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val merge = builder.add(Merge[T](2))
        val balance = builder.add(Balance[T](3))
        merge ~> balance
        Balance2x3[T](merge.in(0), merge.in(1), balance.out(0), balance.out(1), balance.out(2))
      }
    }
  }

  val fastSource = Source
    .repeat[Int](1000)
    .throttle(2, 1.second)

  val slowSource = Source(LazyList.from(1)).throttle(1, 1.second)
  val createSink: Int => Sink[Int, _] = index => Sink.foreach[Int] { element => println(s"[sink $index] received the element $element") }

  val runnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val fastSourceShape = builder.add(fastSource)
      val slowSourceShape = builder.add(slowSource)
      val balance2x3Shape = builder.add(Balance2x3[Int]())
      val sink1Shape = builder.add(createSink(1))
      val sink2Shape = builder.add(createSink(2))
      val sink3Shape = builder.add(createSink(3))

      fastSourceShape ~> balance2x3Shape.in0
      slowSourceShape ~> balance2x3Shape.in1

      balance2x3Shape.out0 ~> sink1Shape
      balance2x3Shape.out1 ~> sink2Shape
      balance2x3Shape.out2 ~> sink3Shape
      ClosedShape
    }
  )
  runnableGraph.run()

}
