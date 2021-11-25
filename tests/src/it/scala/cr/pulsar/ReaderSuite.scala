package cr.pulsar

import cats.effect.IO
import cr.pulsar.Reader.MessageAvailable
import cr.pulsar.Topic.Type
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._

import java.util.UUID

object ReaderSuite extends NeutronSuite {
  test("Reader can check if topic has messages") { client =>
    val hpTopic = Topic.simple("reader-test" + UUID.randomUUID(), Type.Persistent)
    val resources = for {
      prod <- Producer.make[IO, Event](client, hpTopic)
      reader <- Reader.make[IO, Event](client, hpTopic)
    } yield prod -> reader

    resources
      .use {
        case (producer, reader) =>
          for {
            res1 <- reader.messageAvailable
            _ <- producer.send(Event(UUID.randomUUID(), "test"))
            res2 <- reader.messageAvailable
          } yield {
            expect.same(MessageAvailable.No, res1) &&
            expect.same(MessageAvailable.Yes, res2)
          }
      }
  }

  test("Reader can read a message if it exists") { client =>
    val hpTopic = Topic.simple("reader-test" + UUID.randomUUID(), Type.Persistent)
    val event   = Event(UUID.randomUUID(), "test")

    val resources = for {
      prod <- Producer.make[IO, Event](client, hpTopic)
      reader <- Reader.make[IO, Event](client, hpTopic)
    } yield prod -> reader

    resources
      .use {
        case (producer, reader) =>
          for {
            res1 <- reader.read1
            _ <- producer.send(event)
            res2 <- reader.read1
          } yield {
            expect.same(None, res1) &&
            expect.same(Some(event), res2)
          }
      }
  }
}
