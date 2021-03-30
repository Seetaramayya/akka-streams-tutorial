package tutorial.akka.streams.dynamic

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ MergeHub, Sink, Source }

import scala.concurrent.duration._

/**
 * Dynamic stream handling:
 *  https://doc.akka.io/docs/akka/current/stream/stream-dynamic.html
 */
object MergeHubExample extends App {
  implicit val system = ActorSystem("dynamic-stream-handling")
  val materializedSink = MergeHub.source[Int].to(Sink.foreach(println)).run()

  Source(1 to 10).throttle(1, 10.millis).runWith(materializedSink)
  Source(1 to 10).throttle(1, 30.millis).map(_ * 10).runWith(materializedSink)
  Source(101 to 200).filter(_ % 2 != 0).runWith(materializedSink)

}
