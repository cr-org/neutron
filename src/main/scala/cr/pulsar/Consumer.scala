package cr.pulsar

import cats.MonadError
import cats.effect._
import cats.implicits._
import cr.pulsar.internal.FutureLift._
import fs2._
import org.apache.pulsar.client.api.{Message, MessageId}

trait Consumer[F[_]] {
  def subscribe: Stream[F, Message[Array[Byte]]]
  def ack(id: MessageId): F[Unit]
  def nack(id: MessageId): F[Unit]
}

object Consumer {
  def create[F[_]: Concurrent: ContextShift](
      client: PulsarClient.T,
      topic: Topic,
      subs: Subscription
  ): Resource[F, Consumer[F]] = {
    Resource
      .make {
        F.delay(
          client.newConsumer
            .topic(topic.url)
            .subscriptionType(subs.sType)
            .subscriptionName(subs.name)
            .subscribe
        )
      }(c => F.delay(c.closeAsync()).futureLift.void)
      .map { c =>
        new Consumer[F] {
          override def ack(id: MessageId): F[Unit]  = F.delay(c.acknowledge(id))
          override def nack(id: MessageId): F[Unit] = F.delay(c.negativeAcknowledge(id))
          // AK How do we deal with server side schemata? This returns the raw bytes only...
          override def subscribe: Stream[F, Message[Array[Byte]]] =
            Stream.repeatEval(
              F.delay(c.receiveAsync()).futureLift
            )
        }
      }
  }

  // TODO: Maybe this belongs somewhere else
  def messageDecoder[F[_]: MonadError[*[_], Throwable], E](
      f: Array[Byte] => Option[E],
      c: Consumer[F]
  ): Pipe[F, Message[Array[Byte]], E] =
    _.evalMap { m =>
      val id = m.getMessageId()
      f(m.getData) match {
        case Some(e) => c.ack(id).as(e)
        case _ =>
          c.nack(id) *> F.raiseError[E](new IllegalArgumentException("Decoding error"))
      }
    }

}
