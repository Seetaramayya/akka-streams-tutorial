package tutorial.akka.streams.dynamic

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.{ Attributes, KillSwitches }

import scala.concurrent.duration._

object KillSwitchExample extends App {
  implicit val system = ActorSystem("kill-switch-example")
  import system.dispatcher
  val counter = Source.fromIterator(() => Iterator.from(1)).withAttributes(Attributes()).throttle(10, 100.millis)

  val killSwitch = counter
    .viaMat(KillSwitches.single[Int])(Keep.right)
    .log("KillSwitch")
    .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel, onFailure = Logging.ErrorLevel, onFinish = Logging.InfoLevel))
    .to(Sink.ignore)
    .run()

  system.scheduler.scheduleOnce(300.millis) {
    killSwitch.shutdown()
  }

}
