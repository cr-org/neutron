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
import scala.annotation.implicitNotFound

/**
  * Basic Pulsar configuration to establish
  * a connection.
  */
sealed abstract class Config {
  val tenant: PulsarTenant
  val namespace: PulsarNamespace
  val url: PulsarURL
}

object Config {
  @newtype case class PulsarTenant(value: String)
  @newtype case class PulsarNamespace(value: String)
  @newtype case class PulsarURL(value: String)

  /**************** Type-level builder ******************/
  sealed trait Info
  object Info {
    sealed trait Empty extends Info
    sealed trait Namespace extends Info
    sealed trait Tenant extends Info
    sealed trait URL extends Info

    type Mandatory = Empty with Namespace with Tenant with URL
  }

  case class ConfigBuilder[I <: Info] protected (
      _tenant: PulsarTenant = PulsarTenant(""),
      _namespace: PulsarNamespace = PulsarNamespace(""),
      _url: PulsarURL = PulsarURL("")
  ) {
    def withTenant(tenant: PulsarTenant): ConfigBuilder[I with Info.Tenant] =
      this.copy(_tenant = tenant)

    def withNameSpace(namespace: PulsarNamespace): ConfigBuilder[I with Info.Namespace] =
      this.copy(_namespace = namespace)

    def withURL(url: PulsarURL): ConfigBuilder[I with Info.URL] =
      this.copy(_url = url)

    /**
      * It creates a new configuration.
      */
    def build(
        implicit @implicitNotFound(
          "Tenant, Namespace and URL are mandatory. To create a default configuration, use Config.Builder.default instead."
        ) ev: I =:= Info.Mandatory
    ): Config =
      new Config {
        val tenant    = _tenant
        val namespace = _namespace
        val url       = _url
      }

    /**
      * It creates a new configuration with the following default values:
      *
      * - tenant: "public"
      * - namespace: "default"
      * - url: "pulsar://localhost:6650"
      */
    def default: Config =
      Config.Builder
        .withTenant(PulsarTenant("public"))
        .withNameSpace(PulsarNamespace("default"))
        .withURL(PulsarURL("pulsar://localhost:6650"))
        .build

  }

  object Builder extends ConfigBuilder[Info.Empty]()

}
