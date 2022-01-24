package cr.pulsar

import cats.effect.{ Deferred, IO, Resource }
import cr.pulsar.schema.circe._
import cr.pulsar.Topic.Type
import cr.pulsar.domain.Event
import fs2.Stream

import java.util.UUID

object ProcessorSuite extends NeutronSuite {
  test("Processor should consume and ack messages") { client =>
    val hpTopic = Topic.simple("processor-suite", Type.Persistent)

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
            val consume   = consumer.process(latch.complete)
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
}
