package cr.pulsar

import cats._
import cats.effect._
import cats.implicits._
import fs2.concurrent.{ Topic => _ }
import java.util.concurrent.TimeUnit
import cr.pulsar.internal.FutureLift._
import org.apache.pulsar.client.api.{ MessageId, ProducerBuilder }

import scala.concurrent.duration.FiniteDuration

trait Publisher[F[_], E] {
  def publish(msg: E): F[MessageId]
  def publishAsync(msg: E): F[MessageId]
}

// TODO: It would be preferable to use server side schema validation
// Pulsar supports this. However, the below would have to change
// quite a bit.
object Publisher {

  sealed trait Batching
  object Batching {
    case class Enabled(maxDelay: FiniteDuration, maxMessages: Int) extends Batching
    case object Disabled extends Batching
  }

  def apply[F[_], E](implicit ev: Publisher[F, E]): Publisher[F, E] = ev

  def withLogger[
      F[_]: ContextShift: Parallel: Concurrent,
      E: Inject[*, Array[Byte]]
  ](
      client: PulsarClient.T,
      topic: Topic,
      batching: Batching,
      blocker: Blocker,
      logAction: Array[Byte] => Topic.URL => F[Unit]
  ): Resource[F, Publisher[F, E]] = {
    def configureBatching(
        batching: Batching,
        producerBuilder: ProducerBuilder[Array[Byte]]
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

    lazy val resource = Resource.make {
      blocker.delay(
        configureBatching(
          batching,
          client.newProducer.topic(topic.url.value)
        ).create
      )
    }(p => blocker.delay(p.close()))

    resource.map { prod =>
      new Publisher[F, E] {
        val serialise: E => Array[Byte] = Inject[E, Array[Byte]].inj

        override def publish(msg: E): F[MessageId] = {
          val event = serialise(msg)

          logAction(event)(topic.url) &>
            blocker.delay(prod.send(serialise(msg)))
        }

        override def publishAsync(msg: E): F[MessageId] = {
          val event = serialise(msg)

          logAction(event)(topic.url) &>
            F.delay(prod.sendAsync(serialise(msg))).futureLift
        }
      }
    }
  }

  def create[
      F[_]: ContextShift: Parallel: Concurrent,
      E: Inject[*, Array[Byte]]
  ](
      client: PulsarClient.T,
      topic: Topic,
      batching: Batching,
      blocker: Blocker
  ): Resource[F, Publisher[F, E]] =
    withLogger[F, E](client, topic, batching, blocker, _ => _ => F.unit)

}
