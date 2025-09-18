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

import cats.effect.{ IO, Resource }
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe.circeInstance
import cr.pulsar.schema.utf8._
import org.apache.pulsar.client.api.PulsarClientException.IncompatibleSchemaException

object IncompatibleSchemaSuite extends NeutronSuite {
  test("Incompatible schema types for consumer and producer") { client =>
    val dfTopic = mkTopic

    val res: Resource[IO, (Consumer[IO, Event], Producer[IO, String])] =
      for {
        producer <- Producer.make[IO, String](client, dfTopic)
        consumer <- Consumer.make[IO, Event](client, dfTopic, sub("incompat-err"))
      } yield consumer -> producer

    res.attempt.use {
      case Left(_: IncompatibleSchemaException) => IO.pure(success)
      case _                                    => IO(failure("Expected IncompatibleSchemaException"))
    }
  }
}
