package cr.pulsar

import cats.effect.IO
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._

import scala.concurrent.duration.DurationInt

object ReaderSuite extends NeutronSuite {
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
          res1 <- reader.readUntil(1.second)
          _ <- producer.send(event)
          res2 <- reader.readUntil(1.second)
        } yield {
          expect.same(None, res1) &&
          expect.same(Some(event), res2)
        }
    }
  }
}
