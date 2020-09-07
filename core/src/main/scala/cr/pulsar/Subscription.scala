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

import io.estatico.newtype.macros.newtype
import org.apache.pulsar.client.api.{ SubscriptionMode, SubscriptionType }

// Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
sealed abstract class Subscription {
  val name: Subscription.Name
  val `type`: Subscription.Type
  val mode: Subscription.Mode
  def withType(_type: Subscription.Type): Subscription
  def withMode(_mode: Subscription.Mode): Subscription
}

/**
  * A [[Subscription]] can be one of the following types:
  *
  * - [[Subscription.Type.Exclusive]]
  * - [[Subscription.Type.Failover]]
  * - [[Subscription.Type.KeyShared]]
  * - [[Subscription.Type.Shared]]
  *
  * Find out more at [[https://pulsar.apache.org/docs/en/concepts-messaging/#subscriptions]]
  */
object Subscription {
  @newtype case class Name(value: String)

  sealed trait Mode {
    def pulsarSubscriptionMode: SubscriptionMode
  }

  object Mode {
    case object Durable extends Mode {
      override def pulsarSubscriptionMode: SubscriptionMode = SubscriptionMode.Durable
    }
    case object NonDurable extends Mode {
      override def pulsarSubscriptionMode: SubscriptionMode = SubscriptionMode.NonDurable
    }
  }

  sealed trait Type {
    def pulsarSubscriptionType: SubscriptionType
  }

  object Type {
    case object Exclusive extends Type {
      override def pulsarSubscriptionType: SubscriptionType = SubscriptionType.Exclusive
    }
    case object Shared extends Type {
      override def pulsarSubscriptionType: SubscriptionType = SubscriptionType.Shared
    }
    case object KeyShared extends Type {
      override def pulsarSubscriptionType: SubscriptionType = SubscriptionType.Key_Shared
    }
    case object Failover extends Type {
      override def pulsarSubscriptionType: SubscriptionType = SubscriptionType.Failover
    }
  }

  private case class SubscriptionImpl(
      name: Subscription.Name,
      `type`: Subscription.Type,
      mode: Subscription.Mode
  ) extends Subscription {
    def withType(_type: Subscription.Type): Subscription =
      copy(`type` = _type)
    def withMode(_mode: Subscription.Mode): Subscription =
      copy(mode = _mode)
  }

  /**
    * It creates a subscription with default configuration.
    *
    * - type: Exclusive
    * - mode: Durable
    */
  def apply(name: String): Subscription =
    // Same as Java's defaults: https://github.com/apache/pulsar/blob/master/pulsar-client/src/main/java/org/apache/pulsar/client/impl/conf/ConsumerConfigurationData.java#L62
    SubscriptionImpl(Name(s"$name-subscription"), Type.Exclusive, Mode.Durable)

}
