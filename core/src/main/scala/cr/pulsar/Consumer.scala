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
import cats.syntax.all._
import cr.pulsar.internal.FutureLift._
import fs2._
import org.apache.pulsar.client.api.{
  Consumer => JConsumer,
  MessageId,
  SubscriptionInitialPosition
}
import scala.util.control.NoStackTrace

trait Consumer[F[_], E] {
  def ack(id: MessageId): F[Unit]
  def nack(id: MessageId): F[Unit]
  def subscribe: Stream[F, Consumer.Message[E]]

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

  case class Message[A](id: MessageId, payload: A)
  case class DecodingFailure(bytes: Array[Byte]) extends NoStackTrace

  private def mkConsumer[
      F[_]: Concurrent: ContextShift,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      sub: Subscription,
      topicType: Either[Topic.Pattern, Topic],
      opts: Options[F, E]
  ): Resource[F, Consumer[F, E]] = {
    val acquire =
      F.delay {
        val c = client.newConsumer
        topicType
          .fold(
            p => c.topicsPattern(p.url.value.r.pattern),
            t => c.topic(t.url.value)
          )
          .subscriptionType(sub.`type`.pulsarSubscriptionType)
          .subscriptionName(sub.name.value)
          .subscriptionMode(sub.mode.pulsarSubscriptionMode)
          .subscriptionInitialPosition(opts.initial)
          .subscribeAsync
      }.futureLift

    def release(c: JConsumer[Array[Byte]]) =
      F.delay(c.unsubscribeAsync())
        .futureLift
        .attempt
        .whenA(opts.manualUnsubscribe)
        .as(c.closeAsync())
        .futureLift
        .void

    Resource
      .make(acquire)(release)
      .map { c =>
        new Consumer[F, E] {
          override def ack(id: MessageId): F[Unit]  = F.delay(c.acknowledge(id))
          override def nack(id: MessageId): F[Unit] = F.delay(c.negativeAcknowledge(id))
          override def unsubscribe: F[Unit] =
            F.delay(c.unsubscribeAsync()).futureLift.void
          override def subscribe: Stream[F, Message[E]] =
            Stream.repeatEval(
              F.delay(c.receiveAsync()).futureLift.flatMap { m =>
                val data = m.getData()

                E.prj(data) match {
                  case Some(e) =>
                    opts.logger(e)(Topic.URL(m.getTopicName)) >>
                        ack(m.getMessageId)
                          .whenA(opts.autoAck)
                          .as(Message(m.getMessageId, e))
                  case None =>
                    DecodingFailure(data).raiseError[F, Message[E]]
                }
              }
            )
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
  def multiTopic[
      F[_]: Concurrent: ContextShift,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topicPattern: Topic.Pattern,
      sub: Subscription
  ): Resource[F, Consumer[F, E]] =
    mkConsumer(client, sub, topicPattern.asLeft, Options[F, E]())

  /**
    * It creates a [[Consumer]] with default options.
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def create[
      F[_]: Concurrent: ContextShift,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      sub: Subscription
  ): Resource[F, Consumer[F, E]] =
    withOptions(client, topic, sub, Options[F, E]())

  /**
    * It creates a [[Consumer]] with default options and the supplied message logger.
    */
  def withLogger[
      F[_]: ContextShift: Parallel: Concurrent,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      sub: Subscription,
      logger: E => Topic.URL => F[Unit]
  ): Resource[F, Consumer[F, E]] =
    withOptions(client, topic, sub, Options[F, E]().withLogger(logger))

  /**
    * It creates a [[Consumer]] with the supplied options.
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def withOptions[
      F[_]: Concurrent: ContextShift,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      sub: Subscription,
      opts: Options[F, E]
  ): Resource[F, Consumer[F, E]] =
    mkConsumer(client, sub, topic.asRight, opts)

  // Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
  sealed abstract class Options[F[_], E] {
    val initial: SubscriptionInitialPosition
    val logger: E => Topic.URL => F[Unit]
    val manualUnsubscribe: Boolean
    val autoAck: Boolean

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
      * Automatically `ack` incoming messages
      */
    def withAutoAck: Options[F, E]
  }

  /**
    * Consumer options such as subscription initial position and message logger.
    */
  object Options {
    private case class OptionsImpl[F[_], E](
        initial: SubscriptionInitialPosition,
        logger: E => Topic.URL => F[Unit],
        manualUnsubscribe: Boolean,
        autoAck: Boolean
    ) extends Options[F, E] {
      override def withInitialPosition(
          _initial: SubscriptionInitialPosition
      ): Options[F, E] =
        copy(initial = _initial)

      override def withLogger(_logger: E => (Topic.URL => F[Unit])): Options[F, E] =
        copy(logger = _logger)

      override def withManualUnsubscribe: Options[F, E] =
        copy(manualUnsubscribe = true)

      override def withAutoAck: Options[F, E] =
        copy(autoAck = true)
    }

    def apply[F[_]: Applicative, E](): Options[F, E] = OptionsImpl[F, E](
      SubscriptionInitialPosition.Latest,
      _ => _ => F.unit,
      manualUnsubscribe = false,
      autoAck = false
    )
  }

}
