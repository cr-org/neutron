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

import java.io.{ ByteArrayOutputStream, InputStream }
import java.nio.charset.StandardCharsets.UTF_8

import scala.reflect.ClassTag

import cr.pulsar.Consumer.DecodingFailure
import cr.pulsar.schema.Schema

import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import org.apache.pulsar.client.api.{ Schema => JSchema }
import org.apache.pulsar.client.api.schema.{
  SchemaDefinition,
  SchemaReader,
  SchemaWriter
}

package object circe {

  implicit def circeInstance[T: ClassTag: Decoder: Encoder]: Schema[T] =
    new Schema[T] {
      def schema: JSchema[T] = {
        val reader = new SchemaReader[T] {
          override def read(is: InputStream): T = {
            val bos  = new ByteArrayOutputStream
            val data = new Array[Byte](1024 * 4) // default used by Apache Common's IOUtils

            var nRead: Int = -1

            while ({ nRead = is.read(data, 0, data.length); nRead != -1 }) {
              bos.write(data, 0, nRead)
            }

            read(bos.toByteArray(), 0, 0)
          }

          override def read(bytes: Array[Byte], offset: Int, length: Int): T =
            decode[T](new String(bytes, UTF_8)).fold[T](
              e => throw new DecodingFailure(e.getMessage),
              identity
            )
        }

        val writer = new SchemaWriter[T] {
          override def write(message: T): Array[Byte] =
            message.asJson.noSpaces.getBytes(UTF_8)
        }

        JSchema.JSON[T](
          SchemaDefinition
            .builder()
            .withPojo(implicitly[ClassTag[T]].runtimeClass)
            .withSchemaReader(reader)
            .withSchemaWriter(writer)
            .withSupportSchemaVersioning(true)
            .build()
        )
      }
    }

}
