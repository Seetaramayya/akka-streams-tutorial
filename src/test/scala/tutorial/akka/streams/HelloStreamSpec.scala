package tutorial.akka.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import scala.concurrent.Future

class HelloStreamSpec extends TestKit(ActorSystem("hello-stream-spec")) with AsyncWordSpecLike with Matchers {
  "HelloStream repeat message source" should {
    def takeFirst10(maybeMessage: Option[String] = None): Future[Assertion] = {
      val source = maybeMessage.fold(HelloStream.repeatMessage())(message => HelloStream.repeatMessage(message))
      source.take(10).runWith(Sink.seq[String]).map { emittedElements =>
        emittedElements.size shouldBe 10
        // all the elements should be the same
        val result = emittedElements.zip(emittedElements.tail).forall {
          case (first, second) => first == second
        }
        result shouldBe true
      }
    }

    "emit given message" in {
      takeFirst10(Some("hello test"))
    }
    "emit 'Hello World!' message if no message is provided" in {
      takeFirst10()
    }
  }
}
