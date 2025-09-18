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

import cats.effect.IO
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe._

import scala.concurrent.duration.DurationInt

object ReaderSuite extends NeutronSuite {
  test("Reader can read a message if it exists") { client =>
    val topic = mkTopic
    val event = mkEvent

    val resources = for {
      prod <- Producer.make[IO, Event](client, topic)
      reader <- Reader.make[IO, Event](client, topic)
    } yield prod -> reader

    resources.use {
      case (producer, reader) =>
        for {
          res1 <- reader.readUntil(1.second)
          _ <- producer.send(event)
          res2 <- reader.readUntil(1.second)
        } yield {
          expect.same(None, res1) &&
          expect.same(Some(event), res2)
        }
    }
  }
}
