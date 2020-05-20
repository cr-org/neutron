package cr.pulsar

import cats._
import cats.effect._
import cats.implicits._
import cr.pulsar.Topic.TopicName
import cr.pulsar.internal.FutureLift._
import fs2._
import org.apache.pulsar.client.api.{ Message, MessageId, SubscriptionInitialPosition }

trait Consumer[F[_]] {
  def subscribe: Stream[F, Message[Array[Byte]]]
  def ack(id: MessageId): F[Unit]
  def nack(id: MessageId): F[Unit]
}

object Consumer {
  def create[F[_]: Concurrent: ContextShift](
      client: PulsarClient.T,
      topic: Topic,
      subs: Subscription,
      initial: SubscriptionInitialPosition
  ): Resource[F, Consumer[F]] =
    Resource
      .make {
        F.delay(
            client.newConsumer
              .topic(topic.url)
              .subscriptionType(subs.sType)
              .subscriptionName(subs.name)
              .subscriptionInitialPosition(initial)
              .subscribeAsync
          )
          .futureLift
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

  def loggingMessageDecoder[
      F[_]: MonadError[*[_], Throwable]: Parallel,
      E: Inject[*, Array[Byte]]
  ](
      c: Consumer[F],
      logAction: Array[Byte] => TopicName => F[Unit]
  ): Pipe[F, Message[Array[Byte]], E] =
    _.evalMap { m =>
      val id   = m.getMessageId
      val data = m.getData()

      val acking = E.prj(data) match {
        case Some(e) =>
          c.ack(id).as(e)
        case None =>
          c.nack(id) *> (new IllegalArgumentException("Decoding error")).raiseError[F, E]
      }

      logAction(data)(TopicName(m.getTopicName())) &> acking
    }

  def messageDecoder[
      F[_]: MonadError[*[_], Throwable]: Parallel,
      E: Inject[*, Array[Byte]]
  ](
      c: Consumer[F]
  ): Pipe[F, Message[Array[Byte]], E] =
    loggingMessageDecoder[F, E](c, _ => _ => F.unit)

}
