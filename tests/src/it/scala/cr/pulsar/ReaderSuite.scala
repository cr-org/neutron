package cr.pulsar

import cats.effect.{ IO, Resource }
import cr.pulsar.NeutronSuite.topic
import cr.pulsar.Reader.MessageAvailable
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._
import weaver.IOSuite

import java.util.UUID

object ReaderSuite extends IOSuite {
  override type Res = Pulsar.T
  override def sharedResource: Resource[IO, Res] =
    Pulsar.make[IO](Config.Builder.default.url)

  test("Reader can check if topic has messages") { client =>
    val hpTopic = topic("reader-test" + UUID.randomUUID())

    Producer
      .make[IO, Event](client, hpTopic)
      .parZip(Reader.make[IO, Event](client, hpTopic))
      .use { case (producer, reader) =>
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
    val hpTopic = topic("reader-test" + UUID.randomUUID())
    val event = Event(UUID.randomUUID(), "test")

    Producer
      .make[IO, Event](client, hpTopic)
      .parZip(Reader.make[IO, Event](client, hpTopic))
      .use { case (producer, reader) =>
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
