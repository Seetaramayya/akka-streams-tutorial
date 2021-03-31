package tutorial.akka.streams.dynamic

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class BatchPrinterSinkSpec extends TestKit(ActorSystem("batch-printer-spec")) with AnyWordSpecLike with Matchers {
  "Batch printer sink" should {
    "print any given elements in the batches" in {
      //TODO: how to test ?
      Source(1 to 10).runWith(BatchPrinterSink[Int](10))
    }
  }
}
