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
