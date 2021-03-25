package cr.pulsar

import cr.pulsar.Consumer.DecodingFailure
import cr.pulsar.domain._
import cr.pulsar.schema.Schema

import java.nio.charset.StandardCharsets.UTF_8

import cats.Eq
import cats.effect._
import cats.effect.concurrent.{ Deferred, Ref }
import cats.implicits._
import fs2.Stream
import org.apache.pulsar.client.api.{ Schema => JSchema }
import org.apache.pulsar.common.schema.SchemaInfo
import weaver.IOSuite

object OnFailureSuite extends IOSuite {

  val cfg = Config.Builder.default

  override type Res = Pulsar.T
  override def sharedResource: Resource[IO, Res] = Pulsar.create[IO](cfg.url)

  val sub = (s: String) =>
    Subscription.Builder
      .withName(s)
      .withType(Subscription.Type.Failover)
      .build

  val topic = (s: String) =>
    Topic.Builder
      .withName(s)
      .withConfig(cfg)
      .build

  val batch = Producer.Batching.Disabled
  val shard = (_: Event) => ShardKey.Default

  implicit val eqDecodingFailure: Eq[DecodingFailure] = Eq.by(_.msg)

  implicit val decodingFailsInstance: Schema[String] =
    new Schema[String] {
      def schema: JSchema[String] = new JSchema[String] {
        override def encode(message: String): Array[Byte] = message.getBytes(UTF_8)
        override def decode(bytes: Array[Byte]): String =
          throw new DecodingFailure("Could not decode bytes")
        override def getSchemaInfo(): SchemaInfo = JSchema.BYTES.getSchemaInfo()
      }
    }

  test("A message can't be decoded and the configured action is run") { client =>
    Ref.of[IO, Option[DecodingFailure]](None).flatMap { ref =>
      val hpTopic = topic("on-error-action")

      val opts = Consumer
        .Options[IO, String]()
        .withOnFailure(Consumer.OnFailure.Ack(e => ref.set(Some(e))))

      val res: Resource[IO, (Consumer[IO, String], Producer[IO, String])] =
        for {
          consumer <- Consumer.withOptions(client, hpTopic, sub("decoding-failure"), opts)
          producer <- Producer.create[IO, String](client, hpTopic)
        } yield consumer -> producer

      Deferred[IO, String].flatMap { latch =>
        Stream
          .resource(res)
          .flatMap {
            case (consumer, producer) =>
              val consume =
                consumer.subscribe
                  .evalMap(msg => consumer.ack(msg.id) >> latch.complete(msg.payload))

              val produce =
                Stream("test")
                  .covary[IO]
                  .evalMap(producer.send)
                  .evalMap(_ => latch.get)

              produce
                .concurrently(consume)
                .as(expect(true))
                .handleErrorWith {
                  case e: DecodingFailure =>
                    Stream.eval(ref.get.map(expect.same(_, Some(e))))
                  case e =>
                    Stream(failure(e.getMessage))
                }
          }
          .compile
          .lastOrError
      }
    }
  }

}
