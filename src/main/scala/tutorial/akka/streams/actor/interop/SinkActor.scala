package tutorial.akka.streams.actor.interop

import akka.actor.{ Actor, ActorLogging }
import SinkActor._
import scala.concurrent.duration._

object SinkActor {
  case object StreamInit
  case object StreamAck
  case object StreamClose
  case class StreamFailure(ex: Throwable)
}

class SinkActor extends Actor with ActorLogging {
  import context.dispatcher
  override def receive: Receive = {
    case StreamInit =>
      log.info("Stream initialised")
      sender() ! StreamAck
    case StreamClose =>
      log.info("Stream Stopped!!!")
      context.stop(self)
    case StreamFailure(ex) =>
      log.warning("Stream FAILED !!! {}", ex)
      context.stop(self)
    case message =>
      log.info(s"received a message $message")
      context.system.scheduler.scheduleOnce(1.second, sender(), StreamAck) // is it because of buffer message going to dead-letters?
  }
}
