package cr.pulsar

import cats.effect._
import cr.pulsar.Topic.Type
import cr.pulsar.domain._
import cr.pulsar.schema.circe._
import org.apache.pulsar.client.api.PulsarClientException.IncompatibleSchemaException

object AlwaysIncompatibleSchemaSuite extends NeutronSuite {
  test("ALWAYS_INCOMPATIBLE schemas: producer sends new Event_V2, Consumer expects old Event") { client =>
    val topic = Topic.single("public", "incompat", "test-incompat", Type.Persistent)

    val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event_V2])] =
      for {
        producer <- Producer.make[IO, Event_V2](client, topic)
        consumer <- Consumer.make[IO, Event](client, topic, sub("mysub"))
      } yield consumer -> producer

    res.attempt.use {
      case Left(_: IncompatibleSchemaException) => IO.pure(success)
      case _                                    => IO(failure("Expected IncompatibleSchemaException"))
    }
  }
}
