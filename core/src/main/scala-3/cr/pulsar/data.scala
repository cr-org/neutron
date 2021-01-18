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

object data:
  val OperationTimeout = NewType.of[FiniteDuration]
  type OperationTimeout = OperationTimeout.Type

  val ConnectionTimeout = NewType.of[FiniteDuration]
  type ConnectionTimeout = ConnectionTimeout.Type

  val PulsarTenant = NewType.of[String]
  type PulsarTenant = PulsarTenant.Type

  val PulsarNamespace = NewType.of[String]
  type PulsarNamespace = PulsarNamespace.Type

  val PulsarURL = NewType.of[String]
  type PulsarURL = PulsarURL.Type

  val SubscriptionName = NewType.of[String]
  type SubscriptionName = SubscriptionName.Type

  val TopicName = NewType.of[String]
  type TopicName = TopicName.Type

  val TopicNamePattern = NewType.of[Regex]
  type TopicNamePattern = TopicNamePattern.Type

  val TopicURL = NewType.of[String]
  type TopicURL = TopicURL.Type

  object context:
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
  end context
end data
