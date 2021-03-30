package tutorial.akka.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.{ TestKit, TestProbe }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{ AnyWordSpecLike, AsyncWordSpecLike }

import scala.concurrent.Await
import scala.concurrent.duration._

class AkkaStreamTestkitExampleSpec extends TestKit(ActorSystem("testing-akka-streams")) with AnyWordSpecLike with BeforeAndAfterAll with Matchers {
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)
  private val simpleSource = Source(1 to 10)
  "Actor streams testing in different ways, this specific graph" should {
    "eventually return ∑10" in {
      val simpleSink = Sink.fold[Int, Int](0)(_ + _)
      Await.result(simpleSource.runWith(simpleSink), 1.second) shouldBe 55
    }

    "test with test actor based sink" in {
      val testProbe = TestProbe()
      val probeSink = Sink.actorRef(testProbe.ref, "completion message", t => fail(s"unexpected error ${t.getMessage}"))
      simpleSource.runWith(probeSink)
      testProbe.expectMsgAllOf((1 to 10): _*)
    }

    "test source with test-kit sink" in {
      val probeSink = TestSink.probe[Int]

      val testSubscriber = simpleSource.runWith(probeSink)
      testSubscriber
        .request(4)
        .expectNext(1, 2, 3, 4)
        .request(2)
        .expectNext(5, 6)
        .request(4)
        .expectNext(7, 8, 9, 10)
        .expectComplete()
    }

    "test sink with test-kit source" in {
      val probeSource = TestSource.probe[Int]
      val simpleSink = Sink.seq[Int]

      val (testPublisherProbe, futureElements) = probeSource.toMat(simpleSink)(Keep.both).run()
      testPublisherProbe
        .sendNext(1)
        .sendNext(10)
        .sendNext(100)
        .sendComplete()
      Await.result(futureElements, 1.second) shouldBe Vector(1, 10, 100)
    }

    "test flows with test-kit-source and test-kit-sink" in {
      val flowUnderTest = Flow[Int].map(Math.abs)
      val probeSource = TestSource.probe[Int]
      val probeSink = TestSink.probe[Int]
      val (testPublisherProbe, testSubscriberProbe) = probeSource.via(flowUnderTest).toMat(probeSink)(Keep.both).run()

      testPublisherProbe
        .sendNext(10)
        .sendNext(-10)
        .sendComplete()

      testSubscriberProbe
        .request(1)
        .expectNext(10)
        .request(1)
        .expectNext(10)
        .expectComplete()
    }
  }
}
