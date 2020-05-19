package cr.pulsar

import cats._
import cats.effect._
import cats.implicits._
import fs2.concurrent.{ Topic => _ }
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.ProducerBuilder

trait Publisher[F[_], E] {
  def publish(msg: E): F[Unit]
}

// TODO: It would be preferable to use server side schema validation
// Pulsar supports this. However, the below would have to change
// quite a bit.
object Publisher {

  sealed trait Batching
  object Batching {
    case class Enabled(maxDelayMs: Long, maxMessages: Int) extends Batching
    case object Disabled extends Batching
  }

  def apply[F[_], E](implicit ev: Publisher[F, E]): Publisher[F, E] = ev

  def withLogger[
      F[_]: ContextShift: Parallel: Sync,
      E: Inject[*, Array[Byte]]
  ](
      client: PulsarClient.T,
      topic: Topic,
      batching: Batching,
      blocker: Blocker,
      logAction: Array[Byte] => F[Unit]
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
              delay,
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
          client.newProducer.topic(topic.url)
        ).create
      )
    }(p => blocker.delay(p.close()))

    resource.map { prod =>
      new Publisher[F, E] {
        def serialise = E

        def publish(msg: E): F[Unit] = {
          val event = serialise(msg)

          logAction(event) &>
            blocker.delay(prod.send(serialise(msg))).void
        }
      }
    }
  }

  def create[
      F[_]: ContextShift: Parallel: Sync,
      E: Inject[*, Array[Byte]]
  ](
      client: PulsarClient.T,
      topic: Topic,
      batching: Batching,
      blocker: Blocker
  ): Resource[F, Publisher[F, E]] =
    withLogger[F, E](client, topic, batching, blocker, _ => F.unit)

}
