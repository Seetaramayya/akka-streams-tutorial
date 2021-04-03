package tutorial.akka.streams.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision
import akka.stream.Supervision.{ restartingDecider, resumingDecider, stoppingDecider }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import scala.collection.immutable
import scala.concurrent.Future

class CustomFilterFlowSpec extends TestKit(ActorSystem("custom-flow-spec")) with AsyncWordSpecLike with Matchers {
  private val sampleSource: Source[Int, NotUsed] = Source(1 to 10)
  "Custom filter flow" should {
    "pass the elements when the predicate is satisfied" in {
      Source(1 to 100).via(CustomFilterFlow(_ % 2 == 0)).runWith(Sink.collection).map { elements => elements.size shouldBe 50 }
      Source(List("Akka", "is", "awesome")).via(CustomFilterFlow(_.length > 4)).runWith(Sink.head[String]).map { _ shouldBe "awesome" }
      sampleSource.via(CustomFilterFlow(_ == 4)).via(Flow[Int].map(_ * 2)).runWith(Sink.head[Int]).map { _ shouldBe 8 }
    }

    "fail when the predicate throws exception with 'Resume' strategy" in {
      customFlowTestWithDifferentSupervisionStrategies(resumingDecider, sampleSource).map { elements =>
        system.log.debug("Actual elements are {}", elements)
        elements shouldBe (1 to 10).filterNot(_ == 7)
      }
    }

    "fail when the predicate throws exception with 'Restart' strategy" in {
      customFlowTestWithDifferentSupervisionStrategies(restartingDecider, sampleSource).map { elements =>
        system.log.debug("Actual elements are {}", elements)
        elements shouldBe (1 to 10).filterNot(_ == 7)
      }
    }

    "fail when the predicate throws exception with 'Stop' strategy" in {
      customFlowTestWithDifferentSupervisionStrategies(stoppingDecider, sampleSource).map { elements =>
        system.log.debug("Actual elements are {}", elements)
        val expectedElements = (1 to 6) :+ -1
        elements shouldBe expectedElements
      }
    }
  }

  private def customFlowTestWithDifferentSupervisionStrategies(
      decider: Supervision.Decider,
      sourceToBeUsed: Source[Int, NotUsed]): Future[immutable.Iterable[Int]] = {
    val predicate: Int => Boolean = x => if (x == 7) throw new IllegalArgumentException("I dont like 7 :)") else true
    val myFilter = CustomFilterFlow(predicate).withAttributes(supervisionStrategy(decider)).recover {
      case _: IllegalArgumentException => -1
    }
    sourceToBeUsed.via(myFilter).runWith(Sink.collection)
  }
}
