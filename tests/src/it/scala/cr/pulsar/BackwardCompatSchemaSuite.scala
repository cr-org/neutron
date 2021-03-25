package cr.pulsar

import java.util.UUID

import cr.pulsar.domain._
import cr.pulsar.schema.circe._

import cats.effect._
import cats.effect.concurrent.{ Deferred, Ref }
import cats.implicits._
import fs2.Stream
import weaver.IOSuite

object BackwardCompatSchemaSuite extends IOSuite {

  val cfg = Config.Builder
    .withTenant("public")
    .withNameSpace("neutron")
    .withURL("pulsar://localhost:6650")
    .build

  override type Res = Pulsar.T
  override def sharedResource: Resource[IO, Res] = Pulsar.create[IO](cfg.url)

  val sub = (s: String) =>
    Subscription.Builder
      .withName(s)
      .withType(Subscription.Type.Failover)
      .build

  val topic = Topic.Builder
    .withName("json-backward")
    .withConfig(cfg)
    .build

  val batch = Producer.Batching.Disabled
  val shard = (_: Event) => ShardKey.Default

  test("BACKWARD compatibility: producer sends old Event, Consumer expects Event_V2") {
    client =>
      val res: Resource[IO, (Consumer[IO, Event_V2], Producer[IO, Event])] =
        for {
          consumer <- Consumer.create[IO, Event_V2](client, topic, sub("circe"))
          producer <- Producer.create[IO, Event](client, topic)
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

                val testEvent = Event(UUID.randomUUID(), "test")

                val events = List.fill(5)(testEvent)

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

}
