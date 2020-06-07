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
