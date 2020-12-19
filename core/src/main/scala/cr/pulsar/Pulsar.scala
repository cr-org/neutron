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

import scala.concurrent.duration._

import cats.effect.{ Resource, Sync }
import cr.pulsar.Pulsar.Options.{ ConnectionTimeout, OperationTimeout }
import cr.pulsar.data._
import io.estatico.newtype.macros.newtype
import org.apache.pulsar.client.api.{ PulsarClient => Underlying }

object Pulsar {

  type T = Underlying

  /**
    * It creates an underlying PulsarClient as a `cats.effect.Resource`.
    *
    * It will be closed once the client is no longer in use or in case of
    * shutdown of the application that makes use of it.
    */
  def create[F[_]: Sync](
      url: PulsarURL
  ): Resource[F, T] = withOptions(url, Options())

  def withOptions[F[_]: Sync](
      url: PulsarURL,
      opts: Options
  ): Resource[F, Underlying] =
    Resource.fromAutoCloseable(
      F.delay(
        Underlying.builder
          .serviceUrl(url.value)
          .connectionTimeout(
            opts.connectionTimeout.value.length.toInt,
            opts.connectionTimeout.value.unit
          )
          .operationTimeout(
            opts.operationTimeout.value.length.toInt,
            opts.operationTimeout.value.unit
          )
          .build
      )
    )

  sealed abstract class Options {
    val connectionTimeout: ConnectionTimeout
    val operationTimeout: OperationTimeout

    /**
      * Set the duration of time to wait for a connection to a broker to be established.
      * If the duration passes without a response from the broker, the connection attempt is dropped.
      */
    def withConnectionTimeout(timeout: ConnectionTimeout): Options

    /**
      * Set the operation timeout <i>(default: 30 seconds)</i>.
      *
      * <p>Producer-create, subscribe and unsubscribe operations will be retried until this interval,
      * after which the operation will be marked as failed
      */
    def withOperationTimeout(timeout: OperationTimeout): Options
  }

  object Options {
    @newtype case class OperationTimeout(value: FiniteDuration)
    @newtype case class ConnectionTimeout(value: FiniteDuration)

    private case class OptionsImpl(
        connectionTimeout: ConnectionTimeout,
        operationTimeout: OperationTimeout
    ) extends Options {
      override def withConnectionTimeout(timeout: ConnectionTimeout): Options =
        copy(connectionTimeout = timeout)

      override def withOperationTimeout(timeout: OperationTimeout): Options =
        copy(operationTimeout = timeout)
    }

    def apply(): Options = OptionsImpl(
      ConnectionTimeout(30.seconds),
      OperationTimeout(30.seconds)
    )
  }
}
