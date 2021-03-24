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

import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

import cr.pulsar.domain._
import cr.pulsar.schema.circe._
import cr.pulsar.schema.utf8._

import cats.effect._
import cats.effect.concurrent.{ Deferred, Ref }
import cats.implicits._
import fs2.Stream
import org.apache.pulsar.client.api.PulsarClientException.IncompatibleSchemaException

class PulsarSpec extends PulsarSuite {

  val sub = (s: String) =>
    Subscription.Builder
      .withName(s)
      .withType(Subscription.Type.Failover)
      .build

  val topic = (s: String) =>
    Topic.Builder
      .withName(s)
      .withConfig(cfg)
      .build

  val batch = Producer.Batching.Disabled
  val shard = (_: Event) => ShardKey.Default

  test("BACKWARD compatibility: producer sends old Event, Consumer expects Event_V2") {
    val neutronCfg = Config.Builder
      .withTenant("public")
      .withNameSpace("neutron")
      .withURL("pulsar://localhost:6650")
      .build

    Pulsar.create[IO](neutronCfg.url).use { client =>
      val _topic = Topic.Builder
        .withName("json-backward")
        .withConfig(neutronCfg)
        .build

      val res: Resource[IO, (Consumer[IO, Event_V2], Producer[IO, Event])] =
        for {
          consumer <- Consumer.create[IO, Event_V2](client, _topic, sub("circe"))
          producer <- Producer.create[IO, Event](client, _topic)
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
                  IO(assertEquals(e, testEvent.toV2))
                }
            }
            .compile
            .drain
      }
    }
  }

  test(
    "ALWAYS_INCOMPATIBLE schemas: producer sends new Event_V2, Consumer expects old Event"
  ) {
    val nopeCfg = Config.Builder
      .withTenant("public")
      .withNameSpace("nope")
      .withURL("pulsar://localhost:6650")
      .build

    Pulsar.create[IO](nopeCfg.url).use { client =>
      val _topic = Topic.Builder
        .withName("json-always-incompatible")
        .withConfig(nopeCfg)
        .build

      val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event_V2])] =
        for {
          consumer <- Consumer.create[IO, Event](client, _topic, sub("circe"))
          producer <- Producer.create[IO, Event_V2](client, _topic)
        } yield consumer -> producer

      res.attempt.use {
        case Left(_: IncompatibleSchemaException) => IO.unit
        case _                                    => IO(fail("Expecting IncompatibleSchemaException"))
      }
    }
  }

  withPulsarClient { client =>
    test("A message is published and consumed successfully using Schema.JSON via Circe") {
      val hpTopic = topic("happy-path-json")

      val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event])] =
        for {
          consumer <- Consumer.create[IO, Event](client, hpTopic, sub("hp-circe"))
          producer <- Producer.create[IO, Event](client, hpTopic)
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
                IO(assertEquals(e, testEvent))
              }
          }
          .compile
          .drain
      }
    }

    test("Incompatible Schemas: producer sends old Event, Consumer expects Event_V2") {
      val vTopic = topic("incompat-json")

      val res: Resource[IO, (Consumer[IO, Event_V2], Producer[IO, Event])] =
        for {
          consumer <- Consumer.create[IO, Event_V2](client, vTopic, sub("v-circe"))
          producer <- Producer.create[IO, Event](client, vTopic)
        } yield consumer -> producer

      res.attempt.use {
        case Left(_: IncompatibleSchemaException) => IO.unit
        case _                                    => IO(fail("Expecting IncompatibleSchemaException"))
      }
    }

    test("A message is published and consumed successfully using Schema.BYTES via Inject") {
      val hpTopic = topic("happy-path-bytes")

      val res: Resource[IO, (Consumer[IO, String], Producer[IO, String])] =
        for {
          consumer <- Consumer.create[IO, String](client, hpTopic, sub("hp-bytes"))
          producer <- Producer.create[IO, String](client, hpTopic)
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
                IO(assertEquals(e, testMessage))
              }
          }
          .compile
          .drain
      }
    }

    test("A consumer fails to decode a message") {
      val dfTopic = topic("decoding-failure")

      val res: Resource[IO, (Consumer[IO, Event], Producer[IO, String])] =
        for {
          consumer <- Consumer.create[IO, Event](client, dfTopic, sub("decoding-err"))
          producer <- Producer.create[IO, String](client, dfTopic)
        } yield consumer -> producer

      Deferred[IO, String].flatMap { latch =>
        Stream
          .resource(res)
          .flatMap {
            case (consumer, producer) =>
              val consume =
                consumer.autoSubscribe
                  .onError {
                    case Consumer.DecodingFailure(data) =>
                      Stream.eval(latch.complete(data))
                  }

              val testMessage = "Consumer will fail to decode this message"

              val produce =
                Stream(testMessage)
                  .covary[IO]
                  .evalMap(producer.send)
                  .evalMap(_ => latch.get)

              produce.concurrently(consume.attempt).evalMap { msg =>
                IO(assert(msg.contains("expected json value")))
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
          c1 <- Consumer.create[IO, Event](client, topic("shared"), makeSub("s1"))
          c2 <- Consumer.create[IO, Event](client, topic("shared"), makeSub("s2"))
          p1 <- Producer.withOptions(client, topic("shared"), opts)
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
        }
    }
  }

}
