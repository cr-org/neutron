package cr.pulsar

import cats.effect.{Resource, Sync}
import org.apache.pulsar.client.api.{PulsarClient => Underlying}

object PulsarClient {
  import Config._

  type T = Underlying

  def create[F[_]: Sync](url: PulsarURL): Resource[F, T] =
    Resource.fromAutoCloseable(
      Sync[F].delay(Underlying.builder.serviceUrl(url.value).build)
    )
}
