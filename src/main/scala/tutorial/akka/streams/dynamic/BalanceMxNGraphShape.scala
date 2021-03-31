package tutorial.akka.streams.dynamic

import akka.actor.{ ActorSystem, Cancellable }
import akka.stream.scaladsl.{ Balance, GraphDSL, Merge, RunnableGraph, Sink, Source }
import akka.stream._
import akka.{ Done, NotUsed }

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 *
 * https://doc.akka.io/docs/akka/current/stream/stream-graphs.html#building-reusable-graph-components
 */
object BalanceMxNGraphShape extends App {
  implicit val system = ActorSystem("custom-graph-shape")

  case class CustomBalance[T](override val inlets: Seq[Inlet[T]], override val outlets: Seq[Outlet[T]]) extends Shape {
    override def deepCopy(): Shape = CustomBalance[T](inlets.map(_.carbonCopy()), outlets.map(_.carbonCopy()))

    def in(index: Int): Inlet[T] =
      if (index >= inlets.size || index < 0) throw new IllegalArgumentException(s"given index ${index} is not exists") else inlets(index)
    def out(index: Int): Outlet[T] =
      if (index >= outlets.size || index < 0) throw new IllegalArgumentException(s"given index ${index} is not exists") else outlets(index)
  }

  object CustomBalance {
    def apply[T](inputPorts: Int, outputPorts: Int): Graph[CustomBalance[T], NotUsed] = {
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val merge = builder.add(Merge[T](inputPorts))
        val balance = builder.add(Balance[T](outputPorts))
        merge ~> balance
        CustomBalance[T](merge.inlets, balance.outlets)
      }
    }
  }

  val fastSource: Source[String, NotUsed] = Source
    .cycle[String](() =>
      Iterator.from(
        List(
          "Akka is awesome",
          "Akka streams is also awesome",
          "Akka is very easy to understand",
          "Streams is not that easy to understand, it took some time")))
    .throttle(1, 800.millis)

  val slowSource: Source[String, Cancellable] = Source.tick(1.second, 1.second, "Message from slow source")
  val createSink: Int => Sink[String, Future[Done]] = index => Sink.foreach[String] { element => println(s"[sink $index] received the element '$element'") }

  RunnableGraph
    .fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val customBalance = builder.add(CustomBalance[String](2, 5))

        fastSource ~> customBalance.in(0)
        slowSource ~> customBalance.in(1)

        customBalance.out(0) ~> createSink(1)
        customBalance.out(1) ~> createSink(2)
        customBalance.out(2) ~> createSink(3)
        customBalance.out(3) ~> createSink(4)
        customBalance.out(4) ~> createSink(5)
        ClosedShape
      }
    )
    .run()
}
