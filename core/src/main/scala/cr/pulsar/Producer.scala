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

trait Producer[F[_], E] {

  /**
    * It sends a message in a synchronous fashion.
    */
  def send(msg: E): F[MessageId]

  /**
    * Same as [[send]] but it discards its output.
    */
  def send_(msg: E): F[Unit]

  /**
    * It sends a message in an asynchronous fashion.
    */
  def sendAsync(msg: E): F[MessageId]

  /**
    * Same as [[sendAsync]] but it discards its output.
    */
  def sendAsync_(msg: E): F[Unit]
}

abstract class DefaultProducer[F[_]: Functor, E] extends Producer[F, E] {
  def send_(msg: E): F[Unit] =
    send(msg).void
  def sendAsync_(msg: E): F[Unit] =
    sendAsync(msg).void
}

object Producer {

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
    * It creates a simple [[Producer]].
    *
    * Produced messages will be logged using the given `logAction`.
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
      logAction: E => Topic.URL => F[Unit]
  ): Resource[F, Producer[F, E]] = {
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
        new DefaultProducer[F, E] {
          val serialise: E => Array[Byte] = E.inj

          private def buildMessage(msg: Array[Byte], key: MessageKey) = {
            val event = prod.newMessage().value(msg)
            key match {
              case MessageKey.Of(k) =>
                event.key(k)
              case MessageKey.Default =>
                event
            }
          }

          override def send(msg: E): F[MessageId] =
            logAction(msg)(topic.url) &> blocker.delay {
                  buildMessage(serialise(msg), shardKey(msg)).send()
                }

          def sendAsync(msg: E): F[MessageId] =
            logAction(msg)(topic.url) &> F.delay {
                  buildMessage(serialise(msg), shardKey(msg)).sendAsync()
                }.futureLift

        }
      }
  }

  /**
    * It creates a simple [[Producer]] with a no-op logger.
    *
    * Produced messages will not be logged.
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
  ): Resource[F, Producer[F, E]] =
    withLogger[F, E](client, topic, shardKey, batching, blocker, _ => _ => F.unit)

}
