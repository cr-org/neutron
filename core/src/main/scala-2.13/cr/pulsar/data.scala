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

import scala.concurrent.duration.FiniteDuration
import scala.util.matching.Regex

import io.estatico.newtype.macros._

object data {
  @newtype case class OperationTimeout(value: FiniteDuration)
  @newtype case class ConnectionTimeout(value: FiniteDuration)

  @newtype case class PulsarTenant(value: String)
  @newtype case class PulsarNamespace(value: String)
  @newtype case class PulsarURL(value: String)

  @newtype case class SubscriptionName(value: String)

  @newtype case class TopicName(value: String)
  @newtype case class TopicNamePattern(value: Regex)
  @newtype case class TopicURL(value: String)

}
