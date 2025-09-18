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
import cr.pulsar.Topic.Type
import cr.pulsar.domain._
import cr.pulsar.schema.circe._
import org.apache.pulsar.client.api.PulsarClientException.IncompatibleSchemaException

object AlwaysIncompatibleSchemaSuite extends NeutronSuite {
  test("ALWAYS_INCOMPATIBLE schemas: producer sends new Event_V2, Consumer expects old Event") { client =>
    val topic = Topic.single("public", "incompat", "test-incompat", Type.Persistent)

    val res: Resource[IO, (Consumer[IO, Event], Producer[IO, Event_V2])] =
      for {
        producer <- Producer.make[IO, Event_V2](client, topic)
        consumer <- Consumer.make[IO, Event](client, topic, sub("mysub"))
      } yield consumer -> producer

    res.attempt.use {
      case Left(_: IncompatibleSchemaException) => IO.pure(success)
      case _                                    => IO(failure("Expected IncompatibleSchemaException"))
    }
  }
}
