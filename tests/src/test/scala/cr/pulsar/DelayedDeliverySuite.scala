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

import cats.effect.{ IO, Ref }
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe.circeInstance

import java.time.{ Duration, Instant }
import java.util.UUID
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

object DelayedDeliverySuite extends NeutronSuite {
  case class ReceivedMessage(event: Event, ts: Instant)

  val delay: FiniteDuration = 2.seconds
  def now: IO[Instant]      = IO(Instant.now)

  val subscription: Subscription =
    Subscription.Builder
      .withName(s"dd-shared-${UUID.randomUUID().toString}")
      .withType(Subscription.Type.Shared)
      .build

  val topic = mkTopic

  test(s"A message is published and consumed after a delay with shared subscription") { client =>
    val resources = for {
      producer <- Producer.make[IO, Event](client, topic)
      consumer <- Consumer.make[IO, Event](client, topic, subscription)
    } yield producer -> consumer

    for {
      start <- now
      ref <- Ref.of[IO, List[ReceivedMessage]](List.empty)

      _ <- resources.use {
            case (producer, consumer) =>
              consumer.autoSubscribe
                .evalMap(e => now.map(ts => ReceivedMessage(e, ts)))
                .evalMap(msg => ref.update(_ :+ msg))
                .take(1)
                .compile
                .drain &> producer.sendDelayed_(mkEvent, delay)
          }

      result <- ref.get
    } yield assert(
      result.exists(r => Duration.between(start, r.ts).toMillis >= delay.toMillis)
    )
  }
}
