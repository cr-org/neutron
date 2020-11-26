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

import cats._
import cats.effect._
import cats.syntax.all._
import cr.pulsar.Reader.Message
import cr.pulsar.internal.FutureLift._
import fs2._
import org.apache.pulsar.client.api.MessageId

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

/**
  * A Reader can be used to read all the messages currently available in a topic.
  */
trait Reader[F[_], E] {
  def read: Stream[F, Message[E]]
  def read1: F[Option[Message[E]]]
  def readUntil(timeout: FiniteDuration): F[Option[Message[E]]]
}

object Reader {

  case class DecodingFailure(bytes: Array[Byte]) extends NoStackTrace
  case class Message[A](id: MessageId, key: MessageKey, payload: A)

  def withOptions[
      F[_]: Concurrent: ContextShift,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      opts: Options
  ): Resource[F, Reader[F, E]] =
    Resource
      .make {
        F.delay {
          client.newReader
            .topic(topic.url.value)
            .startMessageId(opts.startMessageId)
            .startMessageIdInclusive()
            .readCompacted(opts.readCompacted)
            .create()
        }
      }(
        c => F.delay(c.closeAsync()).void
      )
      .map { c =>
        new Reader[F, E] {
          override def read: Stream[F, Message[E]] =
            Stream.repeatEval(
              F.delay(c.readNextAsync()).futureLift.flatMap { m =>
                val data = m.getData()

                E.prj(data) match {
                  case Some(e) => F.pure(Message(m.getMessageId, MessageKey(m.getKey), e))
                  case None    => DecodingFailure(data).raiseError[F, Message[E]]
                }
              }
            )

          override def read1: F[Option[Message[E]]] =
            F.delay(c.hasMessageAvailableAsync).futureLift.map(Boolean.unbox).flatMap {
              case false => F.pure(None)
              case true =>
                F.delay(c.readNextAsync()).futureLift.flatMap { m =>
                  val data = m.getData()

                  E.prj(data) match {
                    case Some(e) =>
                      F.pure(Some(Message(m.getMessageId, MessageKey(m.getKey), e)))
                    case None => DecodingFailure(data).raiseError[F, Option[Message[E]]]
                  }
                }
            }

          override def readUntil(timeout: FiniteDuration): F[Option[Message[E]]] =
            F.delay(c.readNext(timeout.length.toInt, timeout.unit)).flatMap { m =>
              Option(m).map(_.getData).flatTraverse { data =>
                E.prj(data) match {
                  case Some(e) =>
                    F.pure(Option(Message(m.getMessageId, MessageKey(m.getKey), e)))
                  case None => DecodingFailure(data).raiseError[F, Option[Message[E]]]
                }
              }
            }
        }
      }

  /**
    * It creates a simple [[Reader]].
    */
  def create[
      F[_]: Concurrent: ContextShift,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic
  ): Resource[F, Reader[F, E]] =
    withOptions[F, E](client, topic, Options())

  // Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
  sealed abstract class Options {
    val startMessageId: MessageId
    val readCompacted: Boolean

    /**
      * The Start message Id. `Latest` by default.
      */
    def withStartMessageId(_startMessageId: MessageId): Options

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
    def withReadCompacted: Options
  }

  /**
    * Consumer options such as subscription initial position and message logger.
    */
  object Options {
    private case class OptionsImpl(
        startMessageId: MessageId,
        readCompacted: Boolean
    ) extends Options {
      override def withStartMessageId(_startMessageId: MessageId): Options =
        copy(startMessageId = _startMessageId)

      override def withReadCompacted: Options =
        copy(readCompacted = true)
    }

    def apply(): Options = OptionsImpl(
      MessageId.latest,
      readCompacted = false
    )
  }

}
