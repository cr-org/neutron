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

import cats.effect.{ Resource, Sync }
import org.apache.pulsar.client.api.{ PulsarClient => Underlying }

import scala.concurrent.duration.FiniteDuration

object Pulsar {
  import Config._

  type T = Underlying

  /**
    * It creates an underlying PulsarClient as a `cats.effect.Resource`.
    *
    * It will be closed once the client is no longer in use or in case of
    * shutdown of the application that makes use of it.
    */
  def create[F[_]: Sync](
      url: PulsarURL,
      timeout: FiniteDuration
  ): Resource[F, T] =
    Resource.fromAutoCloseable(
      F.delay(
        Underlying.builder
          .serviceUrl(url.value)
          .connectionTimeout(timeout.length.toInt, timeout.unit)
          .operationTimeout(timeout.length.toInt, timeout.unit)
          .build
      )
    )
}
