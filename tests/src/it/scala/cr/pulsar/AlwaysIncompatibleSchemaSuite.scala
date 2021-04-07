package cr.pulsar

import cr.pulsar.domain._
import cr.pulsar.schema.circe._

import cats.effect._
import cats.implicits._
import org.apache.pulsar.client.api.PulsarClientException.IncompatibleSchemaException
import weaver.IOSuite

object AlwaysIncompatibleSchemaSuite extends IOSuite {

  val cfg = Config.Builder
    .withTenant("public")
    .withNameSpace("nope")
    .withURL("pulsar://localhost:6650")
    .build

  override type Res = Pulsar.T
  override def sharedResource: Resource[IO, Res] = Pulsar.make[IO](cfg.url)

  val sub = (s: String) =>
    Subscription.Builder
      .withName(s)
      .withType(Subscription.Type.Failover)
      .build

  val topic = Topic.Builder
    .withName("json-always-incompatible")
    .withConfig(cfg)
    .build

  val batch = Producer.Batching.Disabled
  val shard = (_: Event) => ShardKey.Default

  test(
    "ALWAYS_INCOMPATIBLE schemas: producer sends new Event_V2, Consumer expects old Event"
  ) { client =>
    val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event_V2])] =
      for {
        producer <- Producer.make[IO, Event_V2](client, topic)
        consumer <- Consumer.make[IO, Event](client, topic, sub("circe"))
      } yield consumer -> producer

    res.attempt.use {
      case Left(_: IncompatibleSchemaException) => IO.pure(success)
      case _                                    => IO(failure("Expected IncompatibleSchemaException"))
    }
  }

}
