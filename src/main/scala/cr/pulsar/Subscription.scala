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
import org.apache.pulsar.client.api.SubscriptionType

sealed abstract case class Subscription(name: String, sType: SubscriptionType)

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

  def apply(name: Name, sType: Type) =
    new Subscription(name.value ++ "-subscription", sType.pulsarSubscriptionType) {}

}
