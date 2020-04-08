package cr.pulsar

import cats.effect._
import cats.implicits._
import fs2.concurrent.{Topic => _}
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.ProducerBuilder

trait Publisher[F[_], E] {
  def publish(msg: E): F[Unit]
}

// AK - It would be preferable to use server side schema validation
// Pulsar supports this. However, the below would have to change
// quite a bit. I will leave this as a TODO right now.
object Publisher {

  sealed trait Batching
  object Batching {
    case class Enabled(maxDelayMs: Long, maxMessages: Int) extends Batching
    case object Disabled                                   extends Batching
  }

  def apply[F[_], E](implicit ev: Publisher[F, E]): Publisher[F, E] = ev

  def create[F[_], E](
      client: PulsarClient.T,
      topic: Topic,
      batching: Batching,
      blocker: Blocker,
      serialise: E => Array[Byte]
  )(
      implicit F: ConcurrentEffect[F],
      cs: ContextShift[F]
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
    }(p => F.delay(println("closing publisher")) >> blocker.delay(p.close()))

    resource.map { prod =>
      new Publisher[F, E] {
        def publish(msg: E): F[Unit] =
          blocker.delay(prod.send(serialise(msg))).void
      }
    }
  }
}
