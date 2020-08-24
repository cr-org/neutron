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
import fs2._
import org.apache.pulsar.client.api.{ Message, MessageId, SubscriptionInitialPosition }
import scala.util.control.NoStackTrace

trait Consumer[F[_]] {
  def subscribe: Stream[F, Message[Array[Byte]]]
  def ack(id: MessageId): F[Unit]
  def nack(id: MessageId): F[Unit]
}

object Consumer {

  case class DecodingFailure(msg: String) extends NoStackTrace

  private def mkConsumer[F[_]: Concurrent: ContextShift](
      client: Pulsar.T,
      subs: Subscription,
      initial: SubscriptionInitialPosition,
      topicType: Either[Topic.Pattern, Topic]
  ): Resource[F, Consumer[F]] =
    Resource
      .make {
        F.delay {
          val c = client.newConsumer
          topicType
            .fold(
              p => c.topicsPattern(p.url.value.r.pattern),
              t => c.topic(t.url.value)
            )
            .subscriptionType(subs.sType)
            .subscriptionName(subs.name)
            .subscriptionInitialPosition(initial)
            .subscribeAsync
        }.futureLift
      }(
        c =>
          F.delay(c.unsubscribeAsync())
            .futureLift >> F.delay(c.closeAsync()).futureLift.void
      )
      .map { c =>
        new Consumer[F] {
          override def ack(id: MessageId): F[Unit]  = F.delay(c.acknowledge(id))
          override def nack(id: MessageId): F[Unit] = F.delay(c.negativeAcknowledge(id))
          override def subscribe: Stream[F, Message[Array[Byte]]] =
            Stream.repeatEval(
              F.delay(c.receiveAsync()).futureLift
            )
        }
      }

  /**
    * It creates a simple [[Consumer]] for a multi-topic subscription.
    *
    * Find out more at [[https://pulsar.apache.org/docs/en/concepts-messaging/#multi-topic-subscriptions]]
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def multiTopic[F[_]: Concurrent: ContextShift](
      client: Pulsar.T,
      topicPattern: Topic.Pattern,
      subs: Subscription,
      initial: SubscriptionInitialPosition
  ): Resource[F, Consumer[F]] =
    mkConsumer[F](client, subs, initial, topicPattern.asLeft)

  /**
    * It creates a simple [[Consumer]].
    *
    * Note that this does not create a subscription to any Topic,
    * you can use [[Consumer#subscribe]] for this purpose.
    */
  def create[F[_]: Concurrent: ContextShift](
      client: Pulsar.T,
      topic: Topic,
      subs: Subscription,
      initial: SubscriptionInitialPosition
  ): Resource[F, Consumer[F]] =
    mkConsumer[F](client, subs, initial, topic.asRight)

  /**
    * A simple message decoder that uses a [[cats.Inject]] instance
    * to deserialise consumed messages.
    *
    * Consumed messages will be logged using the given `logAction`.
    */
  def loggingMessageDecoder[
      F[_]: MonadError[*[_], Throwable]: Parallel,
      E: Inject[*, Array[Byte]]
  ](
      c: Consumer[F],
      logAction: E => Topic.URL => F[Unit]
  ): Pipe[F, Message[Array[Byte]], E] =
    _.evalMap { m =>
      val id   = m.getMessageId
      val data = m.getData

      E.prj(data) match {
        case Some(e) =>
          logAction(e)(Topic.URL(m.getTopicName)) &> c.ack(id).as(e)
        case None =>
          c.nack(id) *> DecodingFailure(new String(data, "UTF-8")).raiseError[F, E]
      }
    }

  /**
    * A simple message decoder that uses a [[cats.Inject]] instance
    * to deserialise consumed messages.
    *
    * Messages will not be logged by default.
    */
  def messageDecoder[
      F[_]: MonadError[*[_], Throwable]: Parallel,
      E: Inject[*, Array[Byte]]
  ](
      c: Consumer[F]
  ): Pipe[F, Message[Array[Byte]], E] =
    loggingMessageDecoder[F, E](c, _ => _ => F.unit)

}
