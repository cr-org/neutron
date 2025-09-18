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
package domain

import cats.Eq
import cr.pulsar.schema.circe.JsonSchema
import io.circe._
import io.circe.generic.semiauto._

import java.util.UUID

case class Event_V2(uuid: UUID, value: String, code: Option[Int] = None)

object Event_V2 {
  implicit val eq: Eq[Event_V2] = Eq.by(_.uuid)

  implicit val jsonEncoder: Encoder[Event_V2] = deriveEncoder
  implicit val jsonDecoder: Decoder[Event_V2] = deriveDecoder[Event_V2]

  implicit val jsonSchema: JsonSchema[Event_V2] = JsonSchema.derive
}
