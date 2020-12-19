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
import cr.pulsar.Reader.Message
import cr.pulsar.internal.FutureLift._
import fs2._
import org.apache.pulsar.client.api.{ MessageId, Reader => JReader }

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

/**
  * A MessageReader can be used to read all the messages currently available in a topic.
  */
trait MessageReader[F[_], E] {
  def read: Stream[F, Message[E]]
  def read1: F[Option[Message[E]]]
  def readUntil(timeout: FiniteDuration): F[Option[Message[E]]]
}

/**
  * A Reader can be used to read all the messages currently available in a topic. Only cares about payloads.
  */
trait Reader[F[_], E] {
  def read: Stream[F, E]
  def read1: F[Option[E]]
  def readUntil(timeout: FiniteDuration): F[Option[E]]
}

object Reader {

  case class DecodingFailure(bytes: Array[Byte]) extends NoStackTrace
  case class Message[A](id: MessageId, key: MessageKey, payload: A)

  private def mkPulsarReader[F[_]: Sync](
      client: Pulsar.T,
      topic: Topic,
      opts: Options
  ): Resource[F, JReader[Array[Byte]]] =
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

  private def mkMessageReader[
      F[_]: Async,
      E: Inject[*, Array[Byte]]
  ](c: JReader[Array[Byte]]): MessageReader[F, E] =
    new MessageReader[F, E] {
      override def read: Stream[F, Message[E]] =
        Stream.repeatEval(
          F.delay(c.readNextAsync()).futureLift.flatMap { m =>
            val data = m.getData

            E.prj(data) match {
              case Some(e) => F.pure(Message(m.getMessageId, MessageKey(m.getKey), e))
              case None    => DecodingFailure(data).raiseError[F, Message[E]]
            }
          }
        )

      override def read1: F[Option[Message[E]]] =
        F.delay(c.hasMessageAvailableAsync).futureLift.flatMap { hasAvailable =>
          val readNext = F.delay(c.readNextAsync()).futureLift.flatMap { m =>
            val data = m.getData

            E.prj(data) match {
              case Some(e) =>
                F.pure(Message(m.getMessageId, MessageKey(m.getKey), e))
              case None => DecodingFailure(data).raiseError[F, Message[E]]
            }
          }

          Option.when(hasAvailable)(readNext).sequence
        }

      override def readUntil(timeout: FiniteDuration): F[Option[Message[E]]] =
        F.delay(c.hasMessageAvailableAsync).futureLift.flatMap { hasAvailable =>
          val readNext =
            F.delay(c.readNext(timeout.length.toInt, timeout.unit)).flatMap { m =>
              Option(m).map(_.getData).traverse { data =>
                E.prj(data) match {
                  case Some(e) =>
                    F.pure(Message(m.getMessageId, MessageKey(m.getKey), e))
                  case None => DecodingFailure(data).raiseError[F, Message[E]]
                }
              }
            }

          Option.when(hasAvailable)(readNext).flatSequence
        }
    }

  private def mkPayloadReader[F[_]: Functor, E](m: MessageReader[F, E]): Reader[F, E] =
    new Reader[F, E] {
      override def read: Stream[F, E]  = m.read.map(_.payload)
      override def read1: F[Option[E]] = m.read1.map(_.map(_.payload))
      override def readUntil(timeout: FiniteDuration): F[Option[E]] =
        m.readUntil(timeout).map(_.map(_.payload))
    }

  /**
    * It creates a [[Reader]] with the supplied [[Options]].
    */
  def withOptions[
      F[_]: Async,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      opts: Options
  ): Resource[F, Reader[F, E]] =
    mkPulsarReader[F](client, topic, opts)
      .map(c => mkPayloadReader(mkMessageReader[F, E](c)))

  /**
    * It creates a simple [[Reader]].
    */
  def create[
      F[_]: Async,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic
  ): Resource[F, Reader[F, E]] =
    withOptions[F, E](client, topic, Options())

  /**
    * It creates a [[MessageReader]] with the supplied [[Options]].
    */
  def messageReaderWithOptions[
      F[_]: Async,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic,
      opts: Options
  ): Resource[F, MessageReader[F, E]] =
    mkPulsarReader[F](client, topic, opts).map(mkMessageReader[F, E])

  /**
    * It creates a simple [[MessageReader]].
    */
  def messageReader[
      F[_]: Async,
      E: Inject[*, Array[Byte]]
  ](
      client: Pulsar.T,
      topic: Topic
  ): Resource[F, MessageReader[F, E]] =
    messageReaderWithOptions[F, E](client, topic, Options())

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
