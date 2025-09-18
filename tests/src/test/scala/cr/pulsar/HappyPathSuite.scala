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
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._
import cr.pulsar.schema.utf8._
import fs2.Stream

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

            val testEvent = mkEvent

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
