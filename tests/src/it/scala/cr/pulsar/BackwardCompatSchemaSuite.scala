package cr.pulsar

import cats.effect._
import cats.implicits._
import cr.pulsar.domain._
import cr.pulsar.schema.circe._
import fs2.Stream
import org.apache.pulsar.client.api.PulsarClientException.IncompatibleSchemaException

object BackwardCompatSchemaSuite extends NeutronSuite {
  test("BACKWARD compatibility: producer sends old Event, Consumer expects Event_V2") { client =>
    val topic = mkTopic

    val res: Resource[IO, (Consumer[IO, Event_V2], Producer[IO, Event])] =
      for {
        producer <- Producer.make[IO, Event](client, topic)
        consumer <- Consumer.make[IO, Event_V2](client, topic, sub("circe"))
      } yield consumer -> producer

    (Ref.of[IO, Int](0), Deferred[IO, Event_V2]).tupled.flatMap {
      case (counter, latch) =>
        Stream
          .resource(res)
          .flatMap {
            case (consumer, producer) =>
              val consume =
                consumer.subscribe
                  .evalMap { msg =>
                    consumer.ack(msg.id) >>
                      counter.update(_ + 1) >>
                      counter.get.flatMap {
                        case n if n === 5 => latch.complete(msg.payload)
                        case _            => IO.unit
                      }
                  }

              val testEvent = mkEvent
              val events    = List.fill(5)(testEvent)

              val produce =
                Stream.eval {
                  events.traverse_(producer.send_) >> latch.get
                }

              produce.concurrently(consume).evalMap { e =>
                IO(expect.same(e, testEvent.toV2))
              }
          }
          .compile
          .lastOrError
    }
  }

  test(
    "BACKWARD compatibility: producer sends old Event, Consumer expects Event_V3, should break"
  ) { client =>
    val topic = mkTopic

    val res =
      for {
        producer <- Producer.make[IO, Event](client, topic)
        consumer <- Consumer.make[IO, Event_V3](client, topic, sub("broken-compat"))
      } yield consumer -> producer

    res.attempt.use {
      case Left(_: IncompatibleSchemaException) => IO.pure(success)
      case _                                    => IO(failure("Expected IncompatibleSchemaException"))
    }
  }

}
