package cr.pulsar

import cats.effect.{ IO, Resource }
import cr.pulsar.Topic.Type
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe.circeInstance
import cr.pulsar.schema.utf8._
import org.apache.pulsar.client.api.PulsarClientException.IncompatibleSchemaException

object IncompatibleSchemaSuite extends NeutronSuite {
  test("Incompatible schema types for consumer and producer") { client =>
    val dfTopic = Topic.simple("incompatible-schema-types", Type.Persistent)

    val res: Resource[IO, (Consumer[IO, Event], Producer[IO, String])] =
      for {
        producer <- Producer.make[IO, String](client, dfTopic)
        consumer <- Consumer.make[IO, Event](client, dfTopic, sub("incompat-err"))
      } yield consumer -> producer

    res.attempt.use {
      case Left(_: IncompatibleSchemaException) => IO.pure(success)
      case _                                    => IO(failure("Expected IncompatibleSchemaException"))
    }
  }
}
