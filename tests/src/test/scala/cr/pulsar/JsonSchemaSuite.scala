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
import cr.pulsar.domain.Fruit
import cr.pulsar.domain.Outer.Inner
import cr.pulsar.schema.circe._

object JsonSchemaSuite extends NeutronSuite {
  test("Support for JSONSchema with ADTs") { client =>
    val vTopic = mkTopic

    val res: Resource[IO, (Consumer[IO, Fruit], Producer[IO, Fruit])] =
      for {
        producer <- Producer.make[IO, Fruit](client, vTopic)
        consumer <- Consumer.make[IO, Fruit](client, vTopic, sub("fruits"))
      } yield consumer -> producer

    res.use(_ => IO.pure(success))
  }

  test("Support for JSONSchema with class defined within an object") { client =>
    val vTopic = mkTopic

    val res: Resource[IO, (Consumer[IO, Inner], Producer[IO, Inner])] =
      for {
        producer <- Producer.make[IO, Inner](client, vTopic)
        consumer <- Consumer.make[IO, Inner](client, vTopic, sub("outer-inner"))
      } yield consumer -> producer

    res.use(_ => IO.pure(success))
  }
}
