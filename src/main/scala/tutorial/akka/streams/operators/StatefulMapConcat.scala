package tutorial.akka.streams.operators

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Sink, Source }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

object StatefulMapConcat extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  private val inputFruits = List("banana", "pear", "orange", "deny:banana", "banana", "pear", "banana")
  val fruitsAndDeniedCommands = Source(inputFruits)
  val words = Source("baboon" :: "crocodile" :: "bat" :: "flamingo" :: "hedgehog" :: "beaver" :: Nil)
  val expectedFruitsAndDeniedCommands = List("banana", "pear", "orange", "pear")
  val expectedBWords = List("crocodile", "flamingo", "hedgehog", "baboon", "bat", "beaver")

  val denyFilterFlow = Flow[String].statefulMapConcat { () =>
    var denyList = Set.empty[String]

    { element =>
      if (element.startsWith("deny:")) {
        denyList += element.drop("deny:".size)
        Nil // no element downstream when adding a deny listed keyword
      } else if (denyList(element)) {
        Nil // no element downstream if element is deny listed
      } else {
        element :: Nil
      }
    }
  }

//  fruitsAndDeniedCommands.via(denyFilterFlow).runForeach(println)

  def verify(element: String, state: Vector[String]): (Vector[String], Vector[String]) = element match {
    case e if e.startsWith("b") => (Vector.empty, state :+ e)
    case "-end-"                => (state, state)
    case _                      => (Vector(element), state)
  }

  val bWordsLast = Flow[String].concat(Source.single("-end-")).statefulMapConcat[String] { () =>
    {
      var stashedBWords = Vector.empty[String]
      element => {
        val (tobeSent, state) = verify(element, stashedBWords)
        stashedBWords = state
        tobeSent
      }
    }
  }

  // words.via(bWordsLast).runForeach(println)
  // Expected output for above println:
  //crocodile
  //flamingo
  //hedgehog
  //baboon
  //bat
  //beaver
  private val dropDenyWordsFuture: Future[Seq[String]] = fruitsAndDeniedCommands.via(denyFilterFlow).runWith(Sink.seq)
  private val stashBWordsFuture: Future[Seq[String]] = words.via(bWordsLast).runWith(Sink.seq)

  private val finalFuture = Future.sequence(List(dropDenyWordsFuture, stashBWordsFuture))

  def printStatus(message: String, actualResult: Seq[String], expectedResult: List[String]): Unit =
    if (actualResult == expectedResult) println(s"$message is success")
    else {
      val actualResultString = actualResult.mkString(",")
      val expectedResultString = expectedResult.mkString(",")
      val separator = "           !="
      val lineSize = Math.max(actualResultString.length, expectedResultString.length) + separator.length

      println(s"\n[ERROR] Given result is not matching with expected result")
      println("-" * lineSize)
      println(s"$actualResultString$separator\n$expectedResultString")
      println("-" * lineSize)
    }

  finalFuture.onComplete {
    case Success(List(dropDenyWordsResult, stashBWordsResult)) =>
      printStatus("Drop denied words", dropDenyWordsResult, expectedFruitsAndDeniedCommands)
      printStatus("StashedB words", stashBWordsResult, expectedBWords)
      system.terminate()
    case Success(value) =>
      println(s"[ERROR] Unexpected result $value")
      system.terminate()
    case Failure(t) =>
      t.printStackTrace()
      system.terminate()
  }

}
