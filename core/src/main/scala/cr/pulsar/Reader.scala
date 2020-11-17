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
import org.apache.pulsar.client.api.{ Message, MessageId }
import scala.util.control.NoStackTrace

/**
  * A Reader can be used to read all the messages currently available in a topic.
  */
trait Reader[F[_]] {
  def read: Stream[F, Message[Array[Byte]]]
}

object Reader {

  case class DecodingFailure(msg: String) extends NoStackTrace

  private def mkReader[
      G[_]: Concurrent: ContextShift,
      F[_]: Concurrent: ContextShift
  ](
      client: Pulsar.T,
      topic: Topic,
      messageId: MessageId
  ): Resource[G, Reader[F]] = {
    val acquire =
      G.delay {
        client.newReader
          .topic(topic.url.value)
          .startMessageId(messageId)
          .create()
      }

    Resource
      .make(acquire)(c => G.delay(c.closeAsync()).futureLift.void)
      .map { c =>
        new Reader[F] {
          override def read: Stream[F, Message[Array[Byte]]] =
            Stream.repeatEval(
              F.delay(c.readNextAsync()).futureLift
            )
        }
      }
  }

  /**
    * It creates a simple [[Reader]].
    */
  def create[
      G[_]: Concurrent: ContextShift,
      F[_]: Concurrent: ContextShift
  ](
      client: Pulsar.T,
      topic: Topic,
      messageId: MessageId
  ): Resource[G, Reader[F]] =
    mkReader[G, F](client, topic, messageId)

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
      logAction: E => Topic.URL => F[Unit]
  ): Pipe[F, Message[Array[Byte]], E] =
    _.evalMap { m =>
      val data = m.getData

      E.prj(data) match {
        case Some(e) =>
          logAction(e)(Topic.URL(m.getTopicName)).as(e)
        case None =>
          DecodingFailure(new String(data, "UTF-8")).raiseError[F, E]
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
  ]: Pipe[F, Message[Array[Byte]], E] =
    loggingMessageDecoder[F, E](_ => _ => F.unit)

}
