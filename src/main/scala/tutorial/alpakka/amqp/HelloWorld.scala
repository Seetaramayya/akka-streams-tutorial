package tutorial.alpakka.amqp

import java.nio.charset.StandardCharsets
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{ AmqpSink, AmqpSource }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import akka.{ Done, NotUsed }
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType.FANOUT
import spray.json.{ RootJsonWriter, _ }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._

case class AMQPMessage(message: String, encoding: Option[String])

object HelloWorld extends App {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  private val connectionProvider = AmqpCachedConnectionProvider(AmqpDetailsConnectionProvider("localhost", 5672))
  val DefaultExchangeName = "NavCloud.fanout"

  private val exchangeDeclaration: ExchangeDeclaration = ExchangeDeclaration(DefaultExchangeName, FANOUT.getType).withDurable(durable = true)
  private val writeSettings = AmqpWriteSettings(connectionProvider).withExchange(DefaultExchangeName).withDeclaration(exchangeDeclaration)
  private val amqpSink = AmqpSink(writeSettings)

  def createMessageProperties(userId: UUID) = {
    new AMQP.BasicProperties()
      .builder()
      .deliveryMode(1) // 1 - non-persistent, 2 - persistent.
      .contentEncoding(StandardCharsets.UTF_8.toString) // Content Encoding
      .contentType("application/json") // Content Type
      .headers(Map[String, AnyRef]("x-navcloud-uuid" -> userId.toString).asJava)
      .build()
  }
  import User._

  def publish[T: RootJsonWriter](userId: UUID, routingKey: String, message: T): Future[Done] = {
    Source
      .repeat(message)
      .throttle(1, 1.second)
      .log("publish-message")
      .map(m => toCompactJson(m))
      .map(json => WriteMessage(json).withRoutingKey(routingKey).withProperties(createMessageProperties(userId)))
      .runWith(amqpSink)
  }

  def consumer2(): Source[(Int, String), NotUsed] = {
    val fanoutSize = 4
    val amqpSource2 = AmqpSource
      .atMostOnceSource(
        TemporaryQueueSourceSettings(connectionProvider, DefaultExchangeName).withDeclaration(exchangeDeclaration),
        bufferSize = 1
      )
    (0 until fanoutSize).foldLeft(Source.empty[(Int, String)]) {
      case (source, fanoutBranch) => source.merge(amqpSource2.map(msg => (fanoutBranch, msg.bytes.utf8String)))
    }
  }

  def toCompactJson[T: RootJsonWriter](message: T): ByteString = ByteString(message.toJson.compactPrint.getBytes(StandardCharsets.UTF_8))

  def consume(routingKey: String)(implicit mat: Materializer, ec: ExecutionContext): Source[AMQPMessage, NotUsed] = {
    val amqpSource: Source[ReadResult, NotUsed] =
      AmqpSource
        .atMostOnceSource(TemporaryQueueSourceSettings(connectionProvider, DefaultExchangeName).withDeclaration(exchangeDeclaration), 50)
    amqpSource
      .filter(_.envelope.getRoutingKey == routingKey)
      .map(message => AMQPMessage(message.bytes.utf8String, Option(message.properties.getContentEncoding)))
  }

  println("sending message...")
  private val userId: UUID = UUID.fromString("73877d03-fda3-456c-92ab-8e89bb4a552e")
  private val message = User(userId, "seeta", 40)

  publish(userId, s"entities.$userId", message)
  consume(s"entities.$userId").runWith(Sink.foreach(println))

}
