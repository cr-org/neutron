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

import Config._
import io.estatico.newtype.macros._

/**
  * Basic Pulsar configuration to establish
  * a connection.
  */
sealed abstract class Config {
  val tenant: PulsarTenant
  val namespace: PulsarNamespace
  val serviceUrl: PulsarURL
  def withTenant(_tenant: PulsarTenant): Config
  def withNamespace(_namespace: PulsarNamespace): Config
  def withURL(_url: PulsarURL): Config
}

object Config {
  @newtype case class PulsarTenant(value: String)
  @newtype case class PulsarNamespace(value: String)
  @newtype case class PulsarURL(value: String)

  private case class ConfigImpl(
      tenant: PulsarTenant,
      namespace: PulsarNamespace,
      serviceUrl: PulsarURL
  ) extends Config {
    def withTenant(_tenant: PulsarTenant): Config =
      copy(tenant = _tenant)
    def withNamespace(_namespace: PulsarNamespace): Config =
      copy(namespace = _namespace)
    def withURL(_serviceUrl: PulsarURL): Config =
      copy(serviceUrl = _serviceUrl)
  }

  def apply(
      tenant: PulsarTenant,
      namespace: PulsarNamespace,
      serviceUrl: PulsarURL
  ): Config = ConfigImpl(tenant, namespace, serviceUrl)

  /**
    * It creates a default configuration.
    *
    * - tenant: "public"
    * - namespace: "default"
    * - url: "pulsar://localhost:6650"
    */
  def Default: Config =
    ConfigImpl(
      PulsarTenant("public"),
      PulsarNamespace("default"),
      PulsarURL("pulsar://localhost:6650")
    )
}
