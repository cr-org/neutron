package cr.pulsar

import cats.effect.{IO, Resource}
import cr.pulsar.NeutronSuite.topic
import cr.pulsar.Reader.MessageAvailable
import cr.pulsar.domain.Event
import weaver.IOSuite
import cr.pulsar.schema.circe._

import java.util.UUID

object ReaderSuite extends IOSuite {
  override type Res = Pulsar.T
  override def sharedResource: Resource[IO, Res] = Pulsar.make[IO](Config.Builder.default.url)

  test("Reader can check if topic has messages") {
    client =>
      val hpTopic = topic("reader-test")

      val resources: Resource[IO, (Reader[IO, Event], Producer[IO, Event])] =
        for {
          producer <- Producer.make[IO, Event](client, hpTopic)
          reader <- Reader.make[IO, Event](client, hpTopic)
        } yield reader -> producer

      resources.use { case (reader, producer) =>
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

  test("Reader can read a message if it exists") {
    client =>
      val hpTopic = topic("reader-test-2")

      val resources: Resource[IO, (Reader[IO, Event], Producer[IO, Event])] =
        for {
          producer <- Producer.make[IO, Event](client, hpTopic)
          reader <- Reader.make[IO, Event](client, hpTopic)
        } yield reader -> producer

      val event = Event(UUID.randomUUID(), "test")

      resources.use { case (reader, producer) =>
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