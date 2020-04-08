package cr.pulsar

import org.apache.pulsar.client.api.SubscriptionType

sealed abstract case class Subscription(name: String, sType: SubscriptionType)

object Subscription {

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

  def apply(name: String, sType: Type) =
    new Subscription(name ++ "-subscription", sType.pulsarSubscriptionType) {}

}
