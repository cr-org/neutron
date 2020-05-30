package cr.pulsar

import cats.effect.{ Resource, Sync }
import org.apache.pulsar.client.api.{ PulsarClient => Underlying }

object PulsarClient {
  import Config._

  type T = Underlying

  /**
    * It creates an underlying PulsarClient as a `cats.effect.Resource`.
    *
    * It will be closed once the client is no longer in use or in case of
    * shutdown of the application that makes use of it.
    */
  def create[F[_]: Sync](url: PulsarURL): Resource[F, T] =
    Resource.fromAutoCloseable(
      F.delay(Underlying.builder.serviceUrl(url.value).build)
    )
}
