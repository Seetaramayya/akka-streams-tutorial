package tutorial.akka.streams.actor.interop

import akka.actor.{ Actor, ActorLogging }

class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case message: String =>
      log.debug(s"received a message $message")
      sender() ! message * 2
    case message: Int =>
      log.info(s"received a message $message")
      sender() ! message * 2
  }
}
