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

package cr.pulsar.schema

import cr.pulsar.Consumer.DecodingFailure

import cats.Inject
import org.apache.pulsar.client.api.{ Schema => JSchema }
import org.apache.pulsar.common.schema.SchemaInfo

trait Schema[A] {
  def get: JSchema[A]
}

object Schema {
  def apply[A: Schema]: Schema[A] = implicitly

  implicit def fromInject[E: Inject[*, Array[Byte]]]: Schema[E] =
    new Schema[E] {
      def get: JSchema[E] =
        new JSchema[E] {
          override def encode(message: E): Array[Byte] = E.inj(message)
          override def decode(bytes: Array[Byte]): E =
            E.prj(bytes)
              .getOrElse(throw new DecodingFailure(s"Could not decode bytes: $bytes"))
          override def getSchemaInfo(): SchemaInfo = JSchema.BYTES.getSchemaInfo()
        }
    }
}
