/*
 * Copyright 2020 Chatroulette
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cr.pulsar

import cats.effect._
import cats.effect.concurrent.{ Deferred, Ref }
import cats.implicits._
import cr.pulsar.Producer.MessageKey
import fs2.Stream
import java.util.UUID

class PulsarSpec extends PulsarSuite {

  val sub   = Subscription("test").withType(Subscription.Type.Failover)
  val topic = Topic("test", cfg)
  val batch = Producer.Batching.Disabled
  val shard = (_: Event) => Producer.MessageKey.Default

  withPulsarClient { client =>
    test("A message is published and consumed successfully") {
      val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event])] =
        for {
          consumer <- Consumer.create[IO, Event](client, topic, sub)
          producer <- Producer.create[IO, Event](client, topic)
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
                IO(assert(e === testEvent))
              }
          }
          .compile
          .drain
      }
    }

    test(
      "A message with key is published and consumed successfully by the right consumer"
    ) {
      val makeSub =
        (n: String) => Subscription(n).withType(Subscription.Type.KeyShared)

      val opts =
        Producer.Options[IO, Event]().withShardKey(_.shardKey).withBatching(batch)

      val res: Resource[
        IO,
        (Consumer[IO, Event], Consumer[IO, Event], Producer[IO, Event])
      ] =
        for {
          c1 <- Consumer.create[IO, Event](client, topic, makeSub("s1"))
          c2 <- Consumer.create[IO, Event](client, topic, makeSub("s2"))
          producer <- Producer.withOptions(client, topic, opts)
        } yield (c1, c2, producer)

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
                        _.forall(_.shardKey === MessageKey.Of(uuids(0).toString))
                      )
                    val pred2: IO[Boolean] =
                      events2.get.map(
                        _.forall(_.shardKey === MessageKey.Of(uuids(1).toString))
                      )
                    Stream.eval((pred1, pred2).mapN { case (p1, p2) => p1 && p2 })
                  }

                  Stream(produce, consume1, consume2)
                    .parJoin(3)
                    .interruptWhen(interrupter)
              }
              .compile
              .drain
        }
    }
  }

}
