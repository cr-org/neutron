package cr.pulsar

import cats._
import cats.effect._
import cats.implicits._
import cr.pulsar.internal.FutureLift._
import fs2.concurrent.{ Topic => _ }
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.{ MessageId, ProducerBuilder }
import scala.concurrent.duration.FiniteDuration

trait Publisher[F[_], E] {
  def publish(msg: E): F[MessageId]
  def publish_(msg: E): F[Unit]
  def publishAsync(msg: E): F[MessageId]
  def publishAsync_(msg: E): F[Unit]
}

object Publisher {

  sealed trait Batching
  object Batching {
    case class Enabled(maxDelay: FiniteDuration, maxMessages: Int) extends Batching
    case object Disabled extends Batching
  }

  /**
    * It creates a simple [[Publisher]].
    *
    * Published messages will be logged using the given `logAction`.
    */
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

        /**
          * It publishes a message in a synchronous fashion by using
          * the given [[cats.effect.Blocker]].
          */
        override def publish(msg: E): F[MessageId] = {
          val event = serialise(msg)

          logAction(event)(topic.url) &> blocker.delay(prod.send(event))
        }

        /**
          * Same as [[publish]] but it discard its output.
          */
        override def publish_(msg: E): F[Unit] =
          publish(msg).void

        /**
          * It publishes a message in an asynchronous fashion.
          */
        override def publishAsync(msg: E): F[MessageId] = {
          val event = serialise(msg)

          logAction(event)(topic.url) &> F.delay(prod.sendAsync(event)).futureLift
        }

        /**
          * Same as [[publishAsync]] but it discard its output.
          */
        override def publishAsync_(msg: E): F[Unit] =
          publishAsync(msg).void
      }
    }
  }

  /**
    * It creates a simple [[Publisher]] with a no-op logger.
    *
    * Published messages will not be logged.
    */
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
