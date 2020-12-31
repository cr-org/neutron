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

object data {
  //opaque type Bar = Int
  //opaque type Foo = String

  case class OperationTimeout(value: FiniteDuration)
  case class ConnectionTimeout(value: FiniteDuration)

  case class PulsarTenant(value: String)
  case class PulsarNamespace(value: String)
  case class PulsarURL(value: String)

  case class SubscriptionName(value: String)

  case class TopicName(value: String)
  case class TopicNamePattern(value: Regex)
  case class TopicURL(value: String)

  object context {
    final case class Tenant(value: String)
    final case class Namespace(value: String)
    final case class FunctionName(value: String)
    final case class FunctionId(value: String)
    final case class InstanceId(value: Int)
    final case class NumInstances(value: Int)
    final case class FunctionVersion(value: String)
    final case class InputTopic(value: String)
    final case class OutputTopic(value: String)
    final case class OutputSchemaType(value: String)
  }
  //opaque type PulsarTenant = String
  //opaque type PulsarNamespace = String
  //opaque type PulsarURL = String

  //extension (x: PulsarTenant) {
  //def value: String = x
  //}
  //extension (x: PulsarNamespace) {
  //def value: String = x
  //}
  //extension (x: PulsarURL) {
  //def value: String = x
  //}
}
