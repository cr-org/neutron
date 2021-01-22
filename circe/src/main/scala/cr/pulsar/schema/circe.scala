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

import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8

import scala.reflect.ClassTag

import cr.pulsar.Consumer.DecodingFailure

import io.circe._
import io.circe.parser.decode
import io.circe.syntax._

import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.schema.{
  SchemaDefinition,
  SchemaReader,
  SchemaWriter
}
import java.io.DataInputStream
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.io.BufferedReader

object Circe {
  def of[T: ClassTag: Encoder: Decoder]: Schema[T] = {
    val reader = new SchemaReader[T] {
      override def read(inputStream: InputStream): T = {
        val bytes = new Array[Byte](inputStream.available())
        read(bytes, 0, 0)
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

    Schema.JSON[T](
      SchemaDefinition
        .builder()
        .withPojo(implicitly[ClassTag[T]].getClass)
        .withSchemaReader(reader)
        .withSchemaWriter(writer)
        .build()
    )
  }
}
