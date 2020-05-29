package cr.pulsar

import Config._
import io.estatico.newtype.macros._

/**
  * Basic Pulsar configuration to establish
  * a connection.
  */
case class Config(
    tenant: PulsarTenant,
    namespace: PulsarNamespace,
    serviceUrl: PulsarURL
)

object Config {
  @newtype case class PulsarTenant(value: String)
  @newtype case class PulsarNamespace(value: String)
  @newtype case class PulsarURL(value: String)
}
