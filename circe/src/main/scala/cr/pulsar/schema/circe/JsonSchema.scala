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

package cr.pulsar.schema.circe

import org.apache.avro.{ Schema => AvroSchema }

trait JsonSchema[A] {
  def avro: AvroSchema
}

object JsonSchema {
  def apply[A: JsonSchema]: JsonSchema[A] = implicitly

  def fromAvro[A](schema: AvroSchema): JsonSchema[A] =
    new JsonSchema[A] {
      def avro: AvroSchema = schema
    }
}
