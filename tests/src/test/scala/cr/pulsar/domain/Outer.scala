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

object Outer {
  case class Inner(code: Int)

  implicit val eq: Eq[Inner] = Eq.by(_.code)

  implicit val jsonEncoder: Encoder[Inner] = deriveEncoder
  implicit val jsonDecoder: Decoder[Inner] = deriveDecoder

  implicit val jsonSchema: JsonSchema[Inner] = JsonSchema.derive
}
