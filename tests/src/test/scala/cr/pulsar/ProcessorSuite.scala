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

import cats.effect.{ Deferred, IO, Resource }
import cr.pulsar.schema.circe._
import cr.pulsar.domain.Event
import fs2.Stream

import java.util.UUID

object ProcessorSuite extends NeutronSuite {
  test("Processor should consume and ack messages") { client =>
    val hpTopic = mkTopic

    val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event])] =
      for {
        producer <- Producer.make[IO, Event](client, hpTopic)
        consumer <- Consumer.make[IO, Event](client, hpTopic, sub("ack-sub"))
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

  test("Message should be nacked if processor failed") { client =>
    val hpTopic = mkTopic

    val res: Resource[IO, (Consumer[IO, Event], Consumer[IO, Event], Producer[IO, Event])] =
      for {
        producer <- Producer.make[IO, Event](client, hpTopic)
        consumer1 <- Consumer.make[IO, Event](client, hpTopic, sub("nack-sub"))
        consumer2 <- Consumer.make[IO, Event](client, hpTopic, sub("ack-sub"))
      } yield (consumer1, consumer2, producer)

    Deferred[IO, Throwable].flatMap { latch1 =>
      Deferred[IO, Event].flatMap { latch2 =>
        Stream
          .resource(res)
          .flatMap {
            case (consumer1, consumer2, producer) =>
              val testEvent = Event(UUID.randomUUID(), "test")
              val error     = new Exception("error!")

              val consume1 =
                consumer1
                  .process(_ => IO.raiseError(error))
                  .handleErrorWith(ex => Stream.eval(latch1.complete(ex)))
              val consume2 = consumer2.process(e => latch2.complete(e).as(e))

              val produce =
                Stream(testEvent)
                  .covary[IO]
                  .evalMap(producer.send)
                  .evalMap(_ => latch1.get.both(latch2.get))

              val consume = consume1.attempt.zip(consume2)

              produce.concurrently(consume).evalMap {
                case (err, ev) => IO(expect.same(err, error) && expect.same(ev, testEvent))
              }
          }
          .compile
          .lastOrError
      }
    }
  }
}
