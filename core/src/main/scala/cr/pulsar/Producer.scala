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

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats._
import cats.effect._
import cats.syntax.all._
import fs2.concurrent.{ Topic => _ }

import cr.pulsar.internal.FutureLift._
import cr.pulsar.internal.TypedMessageBuilderOps._
import cr.pulsar.schema.Schemas
import org.apache.pulsar.client.api.{ MessageId, ProducerBuilder, Schema }

trait Producer[F[_], E] {

  /**
    * Sends a message asynchronously.
    */
  def send(msg: E): F[MessageId]

  /**
    * Sends a message associated with a `key` asynchronously.
    */
  def send(msg: E, key: MessageKey): F[MessageId]

  /**
    * Same as [[send(msg:E)*]] but it discards its output.
    */
  def send_(msg: E): F[Unit]

  /**
    * Same as `send(msg:E,key:MessageKey)` but it discards its output.
    */
  def send_(msg: E, key: MessageKey): F[Unit]
}

object Producer {

  sealed trait Batching
  object Batching {
    final case class Enabled(maxDelay: FiniteDuration, maxMessages: Int) extends Batching
    final case object Disabled extends Batching
  }

  /**
    * It creates a simple [[Producer]] with the supplied options.
    */
  def withOptions[F[_]: Concurrent: ContextShift: Parallel, E](
      client: Pulsar.T,
      topic: Topic,
      schema: Schema[E],
      opts: Options[F, E]
  ): Resource[F, Producer[F, E]] = {
    def configureBatching(
        batching: Batching,
        producerBuilder: ProducerBuilder[E]
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
        F.delay(
          configureBatching(
            opts.batching,
            client.newProducer(schema).topic(topic.url.value)
          ).create
        )
      }(p => F.delay(p.closeAsync()).futureLift.void)
      .map { prod =>
        new Producer[F, E] {
          override def send(msg: E, key: MessageKey): F[MessageId] =
            opts.logger(msg)(topic.url) &> F.delay {
                  prod
                    .newMessage()
                    .value(msg)
                    .withShardKey(opts.shardKey(msg))
                    .withMessageKey(key)
                    .sendAsync()
                }.futureLift

          override def send_(msg: E, key: MessageKey): F[Unit] = send(msg, key).void

          override def send(msg: E): F[MessageId] = send(msg, MessageKey.Empty)

          override def send_(msg: E): F[Unit] = send(msg, MessageKey.Empty).void
        }
      }
  }

  /**
    * It creates a [[Producer]] with default options and the supplied message logger.
    */
  def withLogger[F[_]: ContextShift: Parallel: Concurrent, E](
      client: Pulsar.T,
      topic: Topic,
      schema: Schema[E],
      logger: E => Topic.URL => F[Unit]
  ): Resource[F, Producer[F, E]] =
    withOptions(client, topic, schema, Options[F, E]().withLogger(logger))

  /**
    * It creates a [[Producer]] with default options (no-op logger).
    */
  def create[F[_]: ContextShift: Parallel: Concurrent, E](
      client: Pulsar.T,
      topic: Topic,
      schema: Schema[E]
  ): Resource[F, Producer[F, E]] =
    withOptions(client, topic, schema, Options[F, E]())

  /**
    * It creates a [[Producer]] with the given options and a Schema.BYTES
    * based on the Inject instance.
    */
  def fromInject[
      F[_]: ContextShift: Parallel: Concurrent,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      opts: Options[F, E]
  ): Resource[F, Producer[F, E]] =
    withOptions(client, topic, Schemas.fromInject[E], opts)

  // Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
  sealed abstract class Options[F[_], E] {
    val batching: Batching
    val shardKey: E => ShardKey
    val logger: E => Topic.URL => F[Unit]
    def withBatching(_batching: Batching): Options[F, E]
    def withShardKey(_shardKey: E => ShardKey): Options[F, E]
    def withLogger(_logger: E => Topic.URL => F[Unit]): Options[F, E]
  }

  /**
    * Producer options such as sharding key, batching, and message logger
    */
  object Options {
    private case class OptionsImpl[F[_], E](
        batching: Batching,
        shardKey: E => ShardKey,
        logger: E => Topic.URL => F[Unit]
    ) extends Options[F, E] {
      override def withBatching(_batching: Batching): Options[F, E] =
        copy(batching = _batching)
      override def withShardKey(_shardKey: E => ShardKey): Options[F, E] =
        copy(shardKey = _shardKey)
      override def withLogger(_logger: E => (Topic.URL => F[Unit])): Options[F, E] =
        copy(logger = _logger)
    }
    def apply[F[_]: Applicative, E](): Options[F, E] =
      OptionsImpl[F, E](
        Batching.Disabled,
        _ => ShardKey.Default,
        _ => _ => F.unit
      )
  }

}
