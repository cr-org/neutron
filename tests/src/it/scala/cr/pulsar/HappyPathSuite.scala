package cr.pulsar

import cats.effect.{ Deferred, IO, Resource }
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._
import cr.pulsar.schema.utf8._
import fs2.Stream

import java.util.UUID

object HappyPathSuite extends NeutronSuite {
  test("A message is published and consumed successfully using Schema.JSON via Circe") { client =>
    val hpTopic = mkTopic

    val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event])] =
      for {
        producer <- Producer.make[IO, Event](client, hpTopic)
        consumer <- Consumer.make[IO, Event](client, hpTopic, sub("hp-circe"))
      } yield consumer -> producer

    Deferred[IO, Event].flatMap { latch =>
      Stream
        .resource(res)
        .flatMap {
          case (consumer, producer) =>
            val consume =
              consumer.subscribe
                .evalMap(msg => consumer.ack(msg.id) >> latch.complete(msg.payload))

            val testEvent = Event(UUID.randomUUID(), "test")

            val produce =
              Stream(testEvent)
                .covary[IO]
                .evalMap(producer.send)
                .evalMap(_ => latch.get)

            produce.concurrently(consume).evalMap { e =>
              IO(expect.same(e, testEvent))
            }
        }
        .compile
        .lastOrError
    }
  }

  test("A message is published and consumed successfully using Schema.BYTES via Inject") { client =>
    val hpTopic = mkTopic

    val res: Resource[IO, (Consumer[IO, String], Producer[IO, String])] =
      for {
        producer <- Producer.make[IO, String](client, hpTopic)
        consumer <- Consumer.make[IO, String](client, hpTopic, sub("hp-bytes"))
      } yield consumer -> producer

    Deferred[IO, String].flatMap { latch =>
      Stream
        .resource(res)
        .flatMap {
          case (consumer, producer) =>
            val consume =
              consumer.subscribe
                .evalMap(msg => consumer.ack(msg.id) >> latch.complete(msg.payload))

            val testMessage = "Hello Neutron!"

            val produce =
              Stream(testMessage)
                .covary[IO]
                .evalMap(producer.send)
                .evalMap(_ => latch.get)

            produce.concurrently(consume).evalMap { e =>
              IO(expect.same(e, testMessage))
            }
        }
        .compile
        .lastOrError
    }
  }
}
