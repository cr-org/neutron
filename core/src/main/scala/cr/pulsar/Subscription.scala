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
import scala.annotation.implicitNotFound

sealed abstract class Subscription {
  val name: Subscription.Name
  val `type`: Subscription.Type
  val mode: Subscription.Mode
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

  /**************** Type-level builder ******************/
  sealed trait Info
  object Info {
    sealed trait Empty extends Info
    sealed trait Name extends Info
    sealed trait Mode extends Info
    sealed trait Type extends Info

    type Mandatory = Empty with Name with Mode with Type
  }

  case class SubscriptionBuilder[I <: Info] protected (
      _name: Name = Name(""),
      _type: Type = Type.Exclusive,
      _mode: Mode = Mode.Durable
  ) {
    def withName(name: Name): SubscriptionBuilder[I with Info.Name] =
      this.copy(_name = Name(s"${name.value}-subscription"))

    def withName(name: String): SubscriptionBuilder[I with Info.Name] =
      withName(Name(name))

    def withMode(mode: Mode): SubscriptionBuilder[I with Info.Mode] =
      this.copy(_mode = mode)

    def withType(typ: Type): SubscriptionBuilder[I with Info.Type] =
      this.copy(_type = typ)

    /**
      * It creates a subscription with default configuration.
      *
      * - type: Exclusive
      * - mode: Durable
      */
    def build(
        implicit @implicitNotFound(
          "Subscription.Name is mandatory. By default Type=Exclusive and Mode=Durable."
        ) ev: I =:= Info.Mandatory
    ): Subscription =
      new Subscription {
        val name   = _name
        val `type` = _type
        val mode   = _mode
      }

  }

  object Builder extends SubscriptionBuilder[Info.Empty with Info.Type with Info.Mode]()

}
