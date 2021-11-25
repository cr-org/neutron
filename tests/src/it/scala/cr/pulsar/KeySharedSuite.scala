package cr.pulsar

import cats.effect.{ IO, Ref, Resource }
import cats.syntax.all._
import cr.pulsar.Topic.Type
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._
import fs2.Stream

import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

object KeySharedSuite extends NeutronSuite {
  val topic = Topic.simple("shared", Type.Persistent)

  test("A message with key is published and consumed successfully by the right consumer") { client =>
    val makeSub =
      (n: String) =>
        Subscription.Builder
          .withName(n)
          .withType(Subscription.Type.KeyShared)
          .build

    val opts =
      Producer.Options[IO, Event]().withShardKey(_.shardKey).withBatching(batch)

    val res: Resource[
      IO,
      (Consumer[IO, Event], Consumer[IO, Event], Producer[IO, Event])
    ] =
      for {
        p1 <- Producer.make(client, topic, opts)
        c1 <- Consumer.make[IO, Event](client, topic, makeSub("s1"))
        c2 <- Consumer.make[IO, Event](client, topic, makeSub("s2"))
      } yield (c1, c2, p1)

    (Ref.of[IO, List[Event]](List.empty), Ref.of[IO, List[Event]](List.empty)).tupled
      .flatMap {
        case (events1, events2) =>
          Stream
            .resource(res)
            .flatMap {
              case (c1, c2, producer) =>
                val consume1 =
                  c1.subscribe
                    .evalMap(msg => c1.ack(msg.id) >> events1.update(_ :+ msg.payload))

                val consume2 =
                  c2.subscribe
                    .evalMap(msg => c2.ack(msg.id) >> events2.update(_ :+ msg.payload))

                val uuids = List(UUID.randomUUID(), UUID.randomUUID())

                val events =
                  List.range(1, 6).map(x => Event(uuids(x % 2), "test"))

                val produce =
                  Stream
                    .emits(events)
                    .covary[IO]
                    .evalMap(producer.send_)

                val interrupter = {
                  val pred1: IO[Boolean] =
                    events1.get.map(
                      _.forall(
                        _.shardKey === ShardKey.Of(uuids(0).toString.getBytes(UTF_8))
                      )
                    )
                  val pred2: IO[Boolean] =
                    events2.get.map(
                      _.forall(
                        _.shardKey === ShardKey.Of(uuids(1).toString.getBytes(UTF_8))
                      )
                    )
                  Stream.eval((pred1, pred2).mapN { case (p1, p2) => p1 && p2 })
                }

                Stream(produce, consume1, consume2)
                  .parJoin(3)
                  .interruptWhen(interrupter)
            }
            .compile
            .drain
            .as(success)
      }
  }

}
