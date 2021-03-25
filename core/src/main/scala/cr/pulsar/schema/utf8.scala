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

import java.nio.charset.StandardCharsets.UTF_8

import cr.pulsar.Consumer.DecodingFailure

import cats.implicits._
import org.apache.pulsar.client.api.{ Schema => JSchema }
import org.apache.pulsar.common.schema.SchemaInfo

object utf8 {
  implicit val utf8Instance: Schema[String] =
    new Schema[String] {
      def schema: JSchema[String] = new JSchema[String] {
        override def encode(message: String): Array[Byte] = message.getBytes(UTF_8)
        override def decode(bytes: Array[Byte]): String =
          Either
            .catchNonFatal(new String(bytes, UTF_8))
            .getOrElse(throw new DecodingFailure(s"Could not decode bytes: $bytes"))
        override def getSchemaInfo(): SchemaInfo = JSchema.BYTES.getSchemaInfo()
      }
    }
}
