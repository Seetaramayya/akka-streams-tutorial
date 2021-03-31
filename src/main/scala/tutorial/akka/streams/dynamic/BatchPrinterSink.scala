package tutorial.akka.streams.dynamic

import akka.NotUsed
import akka.stream.scaladsl.Sink
import akka.stream.{ Attributes, Inlet, SinkShape }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler }

import scala.collection.mutable.{ Queue => MutableQueue }

class BatchPrinterSink[T](batchSize: Int) extends GraphStage[SinkShape[T]] {
  val inlet: Inlet[T] = Inlet("BatchPrinter.in")
  override def shape: SinkShape[T] = SinkShape(inlet)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val cache = MutableQueue[T]()

    setHandler(
      inlet,
      new InHandler {
        override def onPush(): Unit = {
          cache.enqueue(grab(inlet))
          if (cache.size >= batchSize) emptyCache()
          pull(inlet)
        }

        override def onUpstreamFinish(): Unit = {
          emptyCache()
          println("Upstream is finished")
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          emptyCache()
          println(s"Upstream is finished with $ex")
        }
      }
    )

    override def preStart(): Unit = pull(inlet)

    private def emptyCache(): Unit = if (cache.nonEmpty) println(s"New Batch is ${cache.dequeueAll(_ => true).mkString("[", ", ", "]")}")
  }
}

object BatchPrinterSink {
  def apply[T](batchSize: Int): Sink[T, NotUsed] = Sink.fromGraph[T, NotUsed](new BatchPrinterSink(batchSize))
}
