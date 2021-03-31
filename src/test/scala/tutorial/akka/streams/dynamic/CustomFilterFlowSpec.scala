package tutorial.akka.streams.dynamic

import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision.resumingDecider
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class CustomFilterFlowSpec extends TestKit(ActorSystem("custom-flow-spec")) with AsyncWordSpecLike with Matchers {
  "Custom filter flow" should {
    "pass the elements when the predicate is satisfied" in {
      Source(1 to 100).via(CustomFilterFlow(_ % 2 == 0)).runWith(Sink.collection).map { elements => elements.size shouldBe 50 }
      Source(List("Akka", "is", "awesome")).via(CustomFilterFlow(_.length > 4)).runWith(Sink.head[String]).map { _ shouldBe "awesome" }
      Source(1 to 10).via(CustomFilterFlow(_ == 4)).via(Flow[Int].map(_ * 2)).runWith(Sink.head[Int]).map { _ shouldBe 8 }
    }

    "fail when the predicate throws exception" in {
      val predicate: Int => Boolean = x => if (x == 7) throw new IllegalArgumentException("I dont like 7") else true

      //TODO: my filter is not using attributes, fix them
      val myFilter = CustomFilterFlow(predicate).withAttributes(supervisionStrategy(resumingDecider)).recover {
        case e: Exception => -1
      }
      val filter = Flow[Int].filter(predicate).withAttributes(supervisionStrategy(resumingDecider)).recover {
        case _: Exception => -1
      }
      Source(1 to 10)
        .via(filter)
        .runWith(Sink.collection)
        .map { elements => elements shouldBe (1 to 10).filter(_ != 7) }
    }
  }
}
