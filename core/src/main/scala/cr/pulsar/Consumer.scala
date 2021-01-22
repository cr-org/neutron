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

import scala.util.control.NoStackTrace

import cr.pulsar.internal.FutureLift._
import cr.pulsar.schema.Schemas

import cats._
import cats.effect._
import cats.syntax.all._
import fs2._
import org.apache.pulsar.client.api.{
  Message,
  MessageId,
  Schema,
  SubscriptionInitialPosition,
  Consumer => JConsumer
}

trait Consumer[F[_], E] {

  /**
    * Acknowledge for a single message.
    */
  def ack(id: MessageId): F[Unit]

  /**
    * Negative acknowledge for a single message.
    */
  def nack(id: MessageId): F[Unit]

  /**
    * It consumes [[Consumer.Message]]s, which contain the ID and the PAYLOAD.
    *
    * If you don't need manual [[ack]]ing, consider using [[autoSubscribe]] instead.
    */
  def subscribe: Stream[F, Consumer.Message[E]]

  /**
    * Auto-ack subscription that consumes the message payload directly.
    */
  def autoSubscribe: Stream[F, E]

  /**
    * This operation fails when performed on a shared subscription where multiple
    * consumers are currently connected.
    *
    * If you do not need a durable subscription, consider using a
    * [[Subscription.Mode.NonDurable]] instead.
    */
  def unsubscribe: F[Unit]
}

object Consumer {

  case class Message[A](id: MessageId, key: MessageKey, payload: A)
  case class DecodingFailure(msg: String) extends Exception(msg) with NoStackTrace

  private def mkConsumer[F[_]: Concurrent: ContextShift, E](
      client: Pulsar.T,
      sub: Subscription,
      topicType: Either[Topic.Pattern, Topic],
      schema: Schema[E],
      opts: Options[F, E]
  ): Resource[F, Consumer[F, E]] = {
    val acquire =
      F.delay {
        val c = client.newConsumer(schema)
        topicType
          .fold(
            p => c.topicsPattern(p.url.value.r.pattern),
            t => c.topic(t.url.value)
          )
          .readCompacted(opts.readCompacted)
          .subscriptionType(sub.`type`.pulsarSubscriptionType)
          .subscriptionName(sub.name.value)
          .subscriptionMode(sub.mode.pulsarSubscriptionMode)
          .subscriptionInitialPosition(opts.initial)
          .subscribeAsync
      }.futureLift

    def release(c: JConsumer[E]): F[Unit] =
      F.delay(c.unsubscribeAsync()).futureLift.attempt.unlessA(opts.manualUnsubscribe) >>
          F.delay(c.closeAsync()).futureLift.void

    Resource
      .make(acquire)(release)
      .map { c =>
        new Consumer[F, E] {
          private def subscribeInternal(autoAck: Boolean): Stream[F, Message[E]] =
            Stream.repeatEval {
              F.delay(c.receiveAsync()).futureLift.flatMap { m =>
                val e = m.getValue()

                opts.logger(e)(Topic.URL(m.getTopicName)) >>
                  ack(m.getMessageId)
                    .whenA(autoAck)
                    .as(Message(m.getMessageId, MessageKey(m.getKey), e))
              }
            }

          override def ack(id: MessageId): F[Unit]  = F.delay(c.acknowledge(id))
          override def nack(id: MessageId): F[Unit] = F.delay(c.negativeAcknowledge(id))
          override def unsubscribe: F[Unit] =
            F.delay(c.unsubscribeAsync()).futureLift.void
          override def subscribe: Stream[F, Message[E]] =
            subscribeInternal(autoAck = false)
          override def autoSubscribe: Stream[F, E] =
            subscribeInternal(autoAck = true).map(_.payload)
        }
      }
  }

  /**
    * It creates a [[Consumer]] for a multi-topic subscription.
    *
    * Find out more at [[https://pulsar.apache.org/docs/en/concepts-messaging/#multi-topic-subscriptions]]
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def multiTopic[F[_]: Concurrent: ContextShift, E](
      client: Pulsar.T,
      topicPattern: Topic.Pattern,
      sub: Subscription,
      schema: Schema[E]
  ): Resource[F, Consumer[F, E]] =
    mkConsumer(client, sub, topicPattern.asLeft, schema, Options[F, E]())

  /**
    * It creates a [[Consumer]] with default options.
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def create[F[_]: Concurrent: ContextShift, E](
      client: Pulsar.T,
      topic: Topic,
      sub: Subscription,
      schema: Schema[E]
  ): Resource[F, Consumer[F, E]] =
    withOptions(client, topic, sub, schema, Options[F, E]())

  /**
    * It creates a [[Consumer]] with default options and the supplied message logger.
    */
  def withLogger[F[_]: ContextShift: Parallel: Concurrent, E](
      client: Pulsar.T,
      topic: Topic,
      sub: Subscription,
      schema: Schema[E],
      logger: E => Topic.URL => F[Unit]
  ): Resource[F, Consumer[F, E]] =
    withOptions(client, topic, sub, schema, Options[F, E]().withLogger(logger))

  /**
    * It creates a [[Consumer]] with the supplied options.
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def withOptions[F[_]: Concurrent: ContextShift, E](
      client: Pulsar.T,
      topic: Topic,
      sub: Subscription,
      schema: Schema[E],
      opts: Options[F, E]
  ): Resource[F, Consumer[F, E]] =
    mkConsumer(client, sub, topic.asRight, schema, opts)

  /**
    * It creates a [[Consumer]] with the supplied options with a Schema.BYTES based
    * on the Inject instance.
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def fromInject[
      F[_]: Concurrent: ContextShift,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      sub: Subscription,
      opts: Options[F, E]
  ): Resource[F, Consumer[F, E]] =
    mkConsumer(client, sub, topic.asRight, Schemas.fromInject[E], opts)

  // Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
  sealed abstract class Options[F[_], E] {
    val initial: SubscriptionInitialPosition
    val logger: E => Topic.URL => F[Unit]
    val manualUnsubscribe: Boolean
    val readCompacted: Boolean

    /**
      * The Subscription Initial Position. `Latest` by default.
      */
    def withInitialPosition(_initial: SubscriptionInitialPosition): Options[F, E]

    /**
      * The logger action that runs on every message consumed. It does nothing by default.
      */
    def withLogger(_logger: E => Topic.URL => F[Unit]): Options[F, E]

    /**
      * Sets unsubscribe mode to `Manual`.
      * If you select this option you will have to call `unsubscribe` manually.
      *
      * Note that the `unsubscribe` operation fails when performed on a shared subscription where
      * multiple consumers are currently connected.
      */
    def withManualUnsubscribe: Options[F, E]

    /**
      * If enabled, the consumer will read messages from the compacted topic rather than reading the full message backlog
      * of the topic. This means that, if the topic has been compacted, the consumer will only see the latest value for
      * each key in the topic, up until the point in the topic message backlog that has been compacted. Beyond that
      * point, the messages will be sent as normal.
      *
      * <p>readCompacted can only be enabled subscriptions to persistent topics, which have a single active consumer
      * (i.e. failure or exclusive subscriptions). Attempting to enable it on subscriptions to a non-persistent topics
      * or on a shared subscription, will lead to the subscription call throwing a PulsarClientException.
      */
    def withReadCompacted: Options[F, E]
  }

  /**
    * Consumer options such as subscription initial position and message logger.
    */
  object Options {
    private case class OptionsImpl[F[_]: Applicative, E](
        initial: SubscriptionInitialPosition,
        logger: E => Topic.URL => F[Unit],
        manualUnsubscribe: Boolean,
        readCompacted: Boolean
    ) extends Options[F, E] {
      override def withInitialPosition(
          _initial: SubscriptionInitialPosition
      ): Options[F, E] =
        copy(initial = _initial)

      override def withLogger(_logger: E => (Topic.URL => F[Unit])): Options[F, E] =
        copy(logger = _logger)

      override def withManualUnsubscribe: Options[F, E] =
        copy(manualUnsubscribe = true)

      override def withReadCompacted: Options[F, E] =
        copy(readCompacted = true)
    }

    def apply[F[_]: Applicative, E](): Options[F, E] = OptionsImpl[F, E](
      SubscriptionInitialPosition.Latest,
      _ => _ => F.unit,
      manualUnsubscribe = false,
      readCompacted = false
    )
  }

}
