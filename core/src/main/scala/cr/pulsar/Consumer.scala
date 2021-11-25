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
import cr.pulsar.schema.Schema
import cats._
import cats.effect._
import cats.syntax.all._
import cr.pulsar.internal.FutureLift
import fs2._
import org.apache.pulsar.client.api.{ DeadLetterPolicy, MessageId, SubscriptionInitialPosition, Consumer => JConsumer }

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

  private def mkConsumer[F[_]: Sync: FutureLift, E: Schema](
      client: Pulsar.Underlying,
      sub: Subscription,
      topic: Topic,
      opts: Options[F, E]
  ): Resource[F, Consumer[F, E]] = {

    val acquire = F.futureLift {
      val c = client.newConsumer(E.schema)
      val z = topic match {
        case s: Topic.Single => c.topic(s.url.value)
        case m: Topic.Multi  => c.topicsPattern(m.url.value.r.pattern)
      }
      opts.deadLetterPolicy
        .fold(z)(z.deadLetterPolicy)
        .readCompacted(opts.readCompacted)
        .subscriptionType(sub.`type`.pulsarSubscriptionType)
        .subscriptionName(sub.name.value)
        .subscriptionMode(sub.mode.pulsarSubscriptionMode)
        .subscriptionInitialPosition(opts.initial)
        .subscribeAsync
    }

    def release(c: JConsumer[E]): F[Unit] =
      F.futureLift(c.unsubscribeAsync()).attempt.unlessA(opts.manualUnsubscribe) >>
          F.futureLift(c.closeAsync()).void

    Resource
      .make(acquire)(release)
      .map { c =>
        new Consumer[F, E] {
          private def subscribeInternal(autoAck: Boolean): Stream[F, Message[E]] =
            Stream.repeatEval {
              F.futureLift(c.receiveAsync()).flatMap { m =>
                val e = m.getValue()

                opts.logger.log(Topic.URL(m.getTopicName), e) >>
                  ack(m.getMessageId)
                    .whenA(autoAck)
                    .as(Message(m.getMessageId, MessageKey(m.getKey), e))
              }

            }

          override def ack(id: MessageId): F[Unit]  = F.delay(c.acknowledge(id))
          override def nack(id: MessageId): F[Unit] = F.delay(c.negativeAcknowledge(id))
          override def unsubscribe: F[Unit] =
            F.futureLift(c.unsubscribeAsync()).void
          override def subscribe: Stream[F, Message[E]] =
            subscribeInternal(autoAck = false)
          override def autoSubscribe: Stream[F, E] =
            subscribeInternal(autoAck = true).map(_.payload)
        }
      }
  }

  /**
    * It creates a [[Consumer]] with the supplied options, or a default value otherwise.
    *
    * A [[Topic]] can either be `Single` or `Multi` for multi topic subscriptions.
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def make[F[_]: Sync: FutureLift, E: Schema](
      client: Pulsar.Underlying,
      topic: Topic,
      sub: Subscription,
      opts: Options[F, E] = null // default value does not work
  ): Resource[F, Consumer[F, E]] = {
    val _opts = Option(opts).getOrElse(Options[F, E]())
    mkConsumer(client, sub, topic, _opts)
  }

  // Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
  sealed abstract class Options[F[_], E] {
    val initial: SubscriptionInitialPosition
    val logger: Logger[F, E]
    val manualUnsubscribe: Boolean
    val readCompacted: Boolean
    val deadLetterPolicy: Option[DeadLetterPolicy]

    /**
      * The Subscription Initial Position. `Latest` by default.
      */
    def withInitialPosition(_initial: SubscriptionInitialPosition): Options[F, E]

    /**
      * The logger action that runs on every message consumed. It does nothing by default.
      */
    def withLogger(_logger: Logger[F, E]): Options[F, E]

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

    /**
      * Set dead letter policy for consumer.
      *
      * By default some message will redelivery so many times possible, even to the extent that it can be never stop.
      * By using dead letter mechanism messages will has the max redelivery count, when message exceeding the maximum
      * number of redeliveries, message will send to the Dead Letter Topic and acknowledged automatic.
      */
    def withDeadLetterPolicy(policy: DeadLetterPolicy): Options[F, E]
  }

  /**
    * Consumer options such as subscription initial position and message logger.
    */
  object Options {
    private case class OptionsImpl[F[_]: Applicative, E](
        initial: SubscriptionInitialPosition,
        logger: Logger[F, E],
        manualUnsubscribe: Boolean,
        readCompacted: Boolean,
        deadLetterPolicy: Option[DeadLetterPolicy]
    ) extends Options[F, E] {
      override def withInitialPosition(
          _initial: SubscriptionInitialPosition
      ): Options[F, E] =
        copy(initial = _initial)

      override def withLogger(_logger: Logger[F, E]): Options[F, E] =
        copy(logger = _logger)

      override def withManualUnsubscribe: Options[F, E] =
        copy(manualUnsubscribe = true)

      override def withReadCompacted: Options[F, E] =
        copy(readCompacted = true)

      override def withDeadLetterPolicy(policy: DeadLetterPolicy): Options[F, E] =
        copy(deadLetterPolicy = Some(policy))
    }

    def apply[F[_]: Applicative, E](): Options[F, E] = OptionsImpl[F, E](
      SubscriptionInitialPosition.Latest,
      Logger.noop[F, E],
      manualUnsubscribe = false,
      readCompacted = false,
      deadLetterPolicy = None
    )
  }

}
