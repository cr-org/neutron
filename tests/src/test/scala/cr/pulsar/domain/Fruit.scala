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

package cr.pulsar.domain

import cats.Eq
import cr.pulsar.schema.circe.JsonSchema
import io.circe._
import io.circe.generic.semiauto._

// simple ADT test for JSON schema support
sealed trait Fruit
object Fruit {
  final case class Banana(origin: String) extends Fruit
  final case class Strawberry(_type: String) extends Fruit
  case object Kiwi extends Fruit

  implicit val eq: Eq[Fruit] = Eq.fromUniversalEquals

  implicit val jsonEncoder: Encoder[Fruit] = deriveEncoder
  implicit val jsonDecoder: Decoder[Fruit] = deriveDecoder

  implicit val jsonSchema: JsonSchema[Fruit] = JsonSchema.derive
}
