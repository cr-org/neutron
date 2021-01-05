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

import scala.annotation.targetName

import cr.pulsar.Config._
import cr.pulsar.data._

abstract class ConfigBuilderExtra[I <: Info] { self: ConfigBuilder[I] =>
  @targetName("withTenant_string")
  def withTenant(tenant: String): ConfigBuilder[I with Info.Tenant] =
    self.withTenant(PulsarTenant(tenant))

  @targetName("withNamespace_string")
  def withNameSpace(namespace: String): ConfigBuilder[I with Info.Namespace] =
    self.withNameSpace(PulsarNamespace(namespace))

  @targetName("withURL_string")
  def withURL(url: String): ConfigBuilder[I with Info.URL] =
    self.withURL(PulsarURL(url))
}

