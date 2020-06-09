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

import cats._
import cats.effect._
import cats.implicits._
import cr.pulsar.internal.FutureLift._
import fs2.concurrent.{ Topic => _ }
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.{ MessageId, ProducerBuilder }
import scala.concurrent.duration.FiniteDuration

trait Publisher[F[_], E] {

  /**
    * It publishes a message in a synchronous fashion.
    */
  def publish(msg: E): F[MessageId]

  /**
    * Same as [[publish]] but it discards its output.
    */
  def publish_(msg: E): F[Unit]

  /**
    * It publishes a message in an asynchronous fashion.
    */
  def publishAsync(msg: E): F[MessageId]

  /**
    * Same as [[publishAsync]] but it discards its output.
    */
  def publishAsync_(msg: E): F[Unit]
}

abstract class DefaultPublisher[F[_]: Functor, E] extends Publisher[F, E] {
  def publish_(msg: E): F[Unit] =
    publish(msg).void
  def publishAsync_(msg: E): F[Unit] =
    publishAsync(msg).void
}

object Publisher {

  sealed trait Batching
  object Batching {
    final case class Enabled(maxDelay: FiniteDuration, maxMessages: Int) extends Batching
    final case object Disabled extends Batching
  }

  sealed trait MessageKey
  object MessageKey {
    final case class Of(value: String) extends MessageKey
    final case object Default extends MessageKey

    implicit val eq: Eq[MessageKey] = Eq.fromUniversalEquals
  }

  /**
    * It creates a simple [[Publisher]].
    *
    * Published messages will be logged using the given `logAction`.
    */
  def withLogger[
      F[_]: ContextShift: Parallel: Concurrent,
      E: Inject[*, Array[Byte]]
  ](
      client: PulsarClient.T,
      topic: Topic,
      shardKey: E => MessageKey, // only needed for key-shared topics
      batching: Batching,
      blocker: Blocker,
      logAction: Array[Byte] => Topic.URL => F[Unit]
  ): Resource[F, Publisher[F, E]] = {
    def configureBatching(
        batching: Batching,
        producerBuilder: ProducerBuilder[Array[Byte]]
    ) =
      batching match {
        case Batching.Enabled(delay, _) =>
          producerBuilder
            .enableBatching(true)
            .batchingMaxPublishDelay(
              delay.toMillis,
              TimeUnit.MILLISECONDS
            )
            .batchingMaxMessages(5)
        case Batching.Disabled =>
          producerBuilder.enableBatching(false)
      }

    Resource
      .make {
        blocker.delay(
          configureBatching(
            batching,
            client.newProducer.topic(topic.url.value)
          ).create
        )
      }(p => blocker.delay(p.close()))
      .map { prod =>
        new DefaultPublisher[F, E] {
          val serialise: E => Array[Byte] = Inject[E, Array[Byte]].inj

          private def buildMessage(msg: Array[Byte], key: MessageKey) = {
            val event = prod.newMessage().value(msg)
            key match {
              case MessageKey.Of(k) =>
                event.key(k)
              case MessageKey.Default =>
                event
            }
          }

          override def publish(msg: E): F[MessageId] = {
            val event = serialise(msg)

            logAction(event)(topic.url) &> blocker.delay {
              buildMessage(event, shardKey(msg)).send()
            }
          }

          def publishAsync(msg: E): F[MessageId] = {
            val event = serialise(msg)

            logAction(event)(topic.url) &> F.delay {
              buildMessage(event, shardKey(msg)).sendAsync()
            }.futureLift
          }

        }
      }
  }

  /**
    * It creates a simple [[Publisher]] with a no-op logger.
    *
    * Published messages will not be logged.
    */
  def create[
      F[_]: ContextShift: Parallel: Concurrent,
      E: Inject[*, Array[Byte]]
  ](
      client: PulsarClient.T,
      topic: Topic,
      shardKey: E => MessageKey,
      batching: Batching,
      blocker: Blocker
  ): Resource[F, Publisher[F, E]] =
    withLogger[F, E](client, topic, shardKey, batching, blocker, _ => _ => F.unit)

}
