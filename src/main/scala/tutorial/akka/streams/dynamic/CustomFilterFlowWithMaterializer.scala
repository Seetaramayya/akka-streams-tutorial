package tutorial.akka.streams.dynamic

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler }

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success }

class CustomFilterFlowWithMaterializer[T, U](f: T => U) extends GraphStageWithMaterializedValue[FlowShape[T, U], Future[Int]] {
  val inlet: Inlet[T] = Inlet("CustomFilterFlowWithMaterializer.in")
  val outlet: Outlet[U] = Outlet("CustomFilterFlowWithMaterializer.out")

  override def shape: FlowShape[T, U] = FlowShape(inlet, outlet)
  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
    val promise = Promise[Int]
    val logic: GraphStageLogic = new GraphStageLogic(shape) {
      var counter = 0
      setHandler(
        outlet,
        new OutHandler {
          override def onPull(): Unit = pull(inlet)

          override def onDownstreamFinish(cause: Throwable): Unit = {
            promise.success(counter)
            super.onDownstreamFinish(cause)
          }
        }
      )

      setHandler(
        inlet,
        new InHandler {
          override def onPush(): Unit = {
            counter += 1
            push(outlet, f(grab(inlet)))
          }

          override def onUpstreamFinish(): Unit = {
            promise.success(counter)
            super.onUpstreamFinish()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }
        }
      )
    }
    (logic, promise.future)
  }
}

object CustomFilterFlowWithMaterializer {
  def apply[T, U](f: T => U): Flow[T, U, Future[Int]] = Flow.fromGraph[T, U, Future[Int]](new CustomFilterFlowWithMaterializer[T, U](f))
}

object Test extends App {
  implicit val system = ActorSystem("custom-flow-with-materializer")

  import system.dispatcher
  val numberOfElementsFuture = Source(1 to 10)
  //    .map(x => if (x == 7) throw new RuntimeException("boom") else x)
    .viaMat(CustomFilterFlowWithMaterializer[Int, Int](_ * 2))(Keep.right)
    .to(Sink.foreach(x => if (x == 14) throw new RuntimeException("boom2") else println(x)))
    .run()

  numberOfElementsFuture.onComplete {
    case Success(count)     => println(s"Number of elements passed are ${count}")
    case Failure(exception) => println(s"Counting number of elements failed with: $exception")
  }
}
