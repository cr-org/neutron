package cr.pulsar

import cats.effect.IO
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._

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
        println("Read0")
        for {
          res1 <- reader.read1
          _ = println("Read1")
          _ <- producer.send(event)
          _ = println("Send1")
          res2 <- reader.read1
          _ = println("Read2")
        } yield {
          expect.same(None, res1) &&
          expect.same(Some(event), res2)
        }
    }
  }
}
