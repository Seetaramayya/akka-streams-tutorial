package tutorial.akka.streams.dynamic

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }

class CustomFilterFlow[T](predicate: T => Boolean) extends GraphStage[FlowShape[T, T]] {
  val inlet: Inlet[T] = Inlet("CustomFilterFlow.in")
  val outlet: Outlet[T] = Outlet("CustomFilterFlow.out")

  override def shape: FlowShape[T, T] = FlowShape(inlet, outlet)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(
      inlet,
      new InHandler {
        override def onPush(): Unit =
          try {
            val nextElement = grab(inlet)
            if (predicate(nextElement)) push(outlet, nextElement)
            else pull(inlet)

          } catch {
            case e: Throwable => failStage(e)
          }
      }
    )

    setHandler(outlet, new OutHandler {
      override def onPull(): Unit = pull(inlet)
    })
  }
}

object CustomFilterFlow {
  def apply[T](predicate: T => Boolean): Flow[T, T, NotUsed] = Flow.fromGraph(new CustomFilterFlow[T](predicate))
}
