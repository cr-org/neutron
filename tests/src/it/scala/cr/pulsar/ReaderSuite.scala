package cr.pulsar

import cats.effect.IO
import cr.pulsar.Reader.MessageAvailable
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._

object ReaderSuite extends NeutronSuite {
  test("Reader can check if topic has messages") { client =>
    val topic = mkTopic
    val event = mkEvent

    val resources = for {
      prod <- Producer.make[IO, Event](client, topic)
      reader <- Reader.make[IO, Event](client, topic)
    } yield prod -> reader

    resources.use {
      case (producer, reader) =>
        for {
          res1 <- reader.messageAvailable
          _ <- producer.send(event)
          res2 <- reader.messageAvailable
        } yield {
          expect.same(MessageAvailable.No, res1) &&
          expect.same(MessageAvailable.Yes, res2)
        }
    }
  }

  test("Reader can read a message if it exists") { client =>
    val topic = mkTopic
    val event = mkEvent

    val resources = for {
      prod <- Producer.make[IO, Event](client, topic)
      reader <- Reader.make[IO, Event](client, topic)
    } yield prod -> reader

    resources.use {
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
