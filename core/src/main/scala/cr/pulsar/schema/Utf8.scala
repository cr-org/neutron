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

import cats.Inject
import cats.syntax.all._

object utf8 {

  implicit val stringBytesInject: Inject[String, Array[Byte]] =
    new Inject[String, Array[Byte]] {
      override val inj: String => Array[Byte]         = _.getBytes(UTF_8)
      override val prj: Array[Byte] => Option[String] = new String(_, UTF_8).some
    }

}
