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

import cr.pulsar.Consumer.DecodingFailure
import cr.pulsar.schema.Schema
import io.circe._
import io.circe.parser.{ decode => jsonDecode }
import io.circe.syntax._
import org.apache.pulsar.client.api.{ Schema => JSchema }
import org.apache.pulsar.client.impl.schema.SchemaInfoImpl
import org.apache.pulsar.common.schema.{ SchemaInfo, SchemaType }

import java.nio.charset.StandardCharsets.UTF_8
import scala.reflect.ClassTag

trait CirceDerivation {
  implicit def circeInstance[T: ClassTag: Decoder: Encoder: JsonSchema]: Schema[T] =
    new Schema[T] {
      def schema: JSchema[T] = new JSchema[T] {
        override def encode(x: T): Array[Byte] =
          x.asJson.noSpaces.getBytes(UTF_8)

        override def decode(bytes: Array[Byte]): T =
          jsonDecode[T](new String(bytes, UTF_8)).fold[T](
            e => throw DecodingFailure(e.getMessage),
            identity
          )

        override def getSchemaInfo(): SchemaInfo =
          new SchemaInfoImpl()
            .setName(implicitly[ClassTag[T]].runtimeClass.getCanonicalName)
            .setType(SchemaType.JSON)
            .setSchema(JsonSchema[T].avro.toString.getBytes(UTF_8))

        override def clone(): JSchema[T] = this
      }
    }
}
