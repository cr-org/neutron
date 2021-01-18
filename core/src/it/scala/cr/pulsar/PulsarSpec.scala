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

import java.util.UUID

import cr.pulsar.schema.utf8._

import cats.effect._
import cats.effect.concurrent.{ Deferred, Ref }
import cats.implicits._
import fs2.Stream

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

  withPulsarClient { client =>
    test("A message is published and consumed successfully") {
      val hpTopic = topic("happy-path")

      val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event])] =
        for {
          consumer <- Consumer.create[IO, Event](client, hpTopic, sub("happy-path"))
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
                IO(assert(e === testEvent))
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
          consumer <- Consumer.create[IO, Event](client, dfTopic, sub("decoding-failure"))
          producer <- Producer.create[IO, String](client, dfTopic)
        } yield consumer -> producer

      Deferred[IO, Array[Byte]].flatMap { latch =>
        Stream
          .resource(res)
          .flatMap {
            case (consumer, producer) =>
              val consume =
                consumer.autoSubscribe
                  .handleErrorWith {
                    case Consumer.DecodingFailure(data) =>
                      Stream.eval(latch.complete(data))
                  }

              val testMessage = "Consumer will fail to decode this message"

              val produce =
                Stream(testMessage)
                  .covary[IO]
                  .evalMap(producer.send)
                  .evalMap(_ => latch.get)

              produce.concurrently(consume).evalMap { msg =>
                IO(assert(stringBytesInject.prj(msg).contains(testMessage)))
              }
          }
          .compile
          .drain
      }
    }

    test(
      "A consumer fails to decode first message, which is acked and logged via onFailure, and continue to process more messages"
    ) {
      (
        Deferred[IO, Either[Throwable, Unit]],
        Ref.of[IO, Option[Event]](None),
        Ref.of[IO, Option[Consumer.DecodingFailure]](None)
      ).tupled
        .flatMap {
          case (latch, ref, logger) =>
            val dfaTopic = topic("decoding-failure-acked-logged")

            val opts = Consumer
              .Options[IO, Event]()
              .withOnFailure(
                Consumer.OnFailure.Ack(e => logger.set(e.some))
              )

            val res: Resource[IO, (Consumer[IO, Event], Producer[IO, String])] =
              for {
                consumer <- Consumer.withOptions(client, dfaTopic, sub("err-acked"), opts)
                producer <- Producer.create[IO, String](client, dfaTopic)
              } yield consumer -> producer

            Stream
              .resource(res)
              .flatMap {
                case (consumer, producer) =>
                  val consume: Stream[IO, Event] =
                    consumer.autoSubscribe
                      .evalTap(e => ref.set(e.some) >> latch.complete(().asRight))
                      .interruptWhen(latch.get)

                  val badMessage = "Consumer will fail to decode this message"
                  val testEvent  = Event(UUID.randomUUID(), "test")

                  val goodMessage = new String(Event.inject.inj(testEvent), "UTF-8")

                  val produce =
                    Stream(badMessage, goodMessage)
                      .covary[IO]
                      .evalMap(producer.send)

                  consume.concurrently(produce).evalMap { _ =>
                    logger.get.map(e => assert(e.nonEmpty)) >>
                      ref.get.map(e => assertEquals(e, Some(testEvent)))
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
          producer <- Producer.withOptions(client, topic("shared"), opts)
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
                        _.forall(
                          _.shardKey === ShardKey.Of(uuids(0).toString.getBytes(charset))
                        )
                      )
                    val pred2: IO[Boolean] =
                      events2.get.map(
                        _.forall(
                          _.shardKey === ShardKey.Of(uuids(1).toString.getBytes(charset))
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
