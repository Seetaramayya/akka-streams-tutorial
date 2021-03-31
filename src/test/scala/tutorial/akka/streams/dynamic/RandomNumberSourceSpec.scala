package tutorial.akka.streams.dynamic

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration._

class RandomNumberSourceSpec extends TestKit(ActorSystem("random-number-generator-source-spec")) with AnyWordSpecLike with Matchers {
  "Random number generator source" should {
    "generate random numbers" in {
      val randomNumbersFuture = RandomNumberSource(100)
        .take(10)
        .runWith(Sink.seq[Int])

      val randomNumbers = Await.result(randomNumbersFuture, 1.second)
      randomNumbers.size shouldBe 10
      // assumption random numbers 70% of the cases it will not repeat otherwise it will be flaky test
      randomNumbers.distinct.size should be > 7
    }

  }
}
