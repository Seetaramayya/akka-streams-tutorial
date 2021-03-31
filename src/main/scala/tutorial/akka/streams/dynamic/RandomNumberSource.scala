package tutorial.akka.streams.dynamic

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage.{ GraphStage, GraphStageLogic, OutHandler }
import akka.stream.{ Attributes, Materializer, Outlet, SourceShape }

import scala.util.Random

/**
 * https://doc.akka.io/docs/akka/current/stream/stream-customize.html
 */
class RandomNumberSource(max: Int) extends GraphStage[SourceShape[Int]] {
  val outlet: Outlet[Int] = Outlet("random-number.out")
  override def shape: SourceShape[Int] = SourceShape(outlet)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(outlet, new OutHandler {
      override def onPull(): Unit = {
        push(outlet, new Random().nextInt(max))
      }
    })
  }
}
object RandomNumberSource {

  def apply(max: Int)(implicit materializer: Materializer): Source[Int, NotUsed] = Source.fromGraph(new RandomNumberSource(max))
}
