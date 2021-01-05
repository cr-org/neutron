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
  opaque type OperationTimeout = FiniteDuration
  object OperationTimeout {
    def apply(v: FiniteDuration): OperationTimeout = v
    extension (x: OperationTimeout) def value: FiniteDuration = x
  }

  opaque type ConnectionTimeout = FiniteDuration
  object ConnectionTimeout {
    def apply(v: FiniteDuration): ConnectionTimeout = v
    extension (x: ConnectionTimeout) def value: FiniteDuration = x
  }

  opaque type PulsarTenant = String
  object PulsarTenant {
    def apply(v: String): PulsarTenant = v
    extension (x: PulsarTenant) def value: String = x
  }

  opaque type PulsarNamespace = String
  object PulsarNamespace {
    def apply(v: String): PulsarNamespace = v
    extension (x: PulsarNamespace) def value: String = x
  }

  opaque type PulsarURL = String
  object PulsarURL {
    def apply(v: String): PulsarURL = v
    extension (x: PulsarURL) def value: String = x
  }

  opaque type SubscriptionName = String
  object SubscriptionName {
    def apply(v: String): SubscriptionName = v
    extension (x: SubscriptionName) def value: String = x
  }

  opaque type TopicName = String
  object TopicName {
    def apply(v: String): TopicName = v
    extension (x: TopicName) def value: String = x
  }

  opaque type TopicNamePattern = Regex
  object TopicNamePattern {
    def apply(v: Regex): TopicNamePattern = v
    extension (x: TopicNamePattern) def value: Regex = x
  }

  opaque type TopicURL = String
  object TopicURL {
    def apply(v: String): TopicURL = v
    extension (x: TopicURL) def value: String = x
  }

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
