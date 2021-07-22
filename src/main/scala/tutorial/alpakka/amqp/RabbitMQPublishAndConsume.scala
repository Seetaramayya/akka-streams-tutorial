package tutorial.alpakka.amqp

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.scaladsl.{ AmqpFlow, AmqpSource }
import akka.stream.alpakka.amqp._
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.util.ByteString
import akka.{ Done, NotUsed }
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._

object User {
  implicit val uuidFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def write(obj: UUID): JsValue = obj.toString.toJson
    override def read(json: JsValue): UUID = json match {
      case JsString(value) => UUID.fromString(value)
      case _               => throw DeserializationException("not valid uuid")
    }
  }

  implicit val format: RootJsonFormat[User] = jsonFormat3(User.apply)
}
case class User(id: UUID, name: String, age: Int)

object RabbitMQPublishAndConsume extends App {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  // For this example cached connection provider is unnecessary.
  // caches the connection it provides to be used in all stages. By default it closes the connection whenever the
  // last stage using the provider stops. Optionally, it takes automaticRelease boolean parameter so the connection
  // is not automatically release and the user have to release it explicitly.
  private val connectionProvider = AmqpCachedConnectionProvider(AmqpDetailsConnectionProvider("localhost", 5672))
  private val queueName = "seeta-test"
  private val queueDeclaration = QueueDeclaration(queueName)
  private val writeSettings = AmqpWriteSettings(connectionProvider)
    .withRoutingKey(queueName)
    .withDeclaration(queueDeclaration)
    .withBufferSize(10)
    .withConfirmationTimeout(200.millis)

  private val readSettings = NamedQueueSourceSettings(connectionProvider, queueName).withDeclaration(queueDeclaration)

  private val amqpFlow = Flow[String].map { message => WriteMessage(ByteString(message)) }.via(AmqpFlow.withConfirmUnordered(writeSettings))

  private val amqpSource: Source[ReadResult, NotUsed] =
    AmqpSource.committableSource(readSettings, 10).mapAsync(10) { cm =>
      val result = Future.successful(cm.message)
      cm.ack()
      result
    }

  def publishMessageToQueue(messages: Vector[String]): Future[Seq[WriteResult]] = Source(messages).via(amqpFlow).runWith(Sink.seq)

  def consumeMessageFromQueue: Future[Done] = amqpSource.map(readResult => readResult.bytes.utf8String).runWith(Sink.foreach(println))

  publishMessageToQueue((1 to 10000).map(i => s"message-$i").toVector)
  consumeMessageFromQueue
}
