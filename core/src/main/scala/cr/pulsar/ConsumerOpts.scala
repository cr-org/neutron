package cr.pulsar

import org.apache.pulsar.client.api.SubscriptionInitialPosition

// Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
sealed abstract class ConsumerOpts {
  val initial: SubscriptionInitialPosition
  def withInitialPosition(initial: SubscriptionInitialPosition): ConsumerOpts
}

object ConsumerOpts {
  private case class ConsumerOptsImpl(
      initial: SubscriptionInitialPosition
  ) extends ConsumerOpts {
    override def withInitialPosition(
        _initial: SubscriptionInitialPosition
    ): ConsumerOpts =
      copy(initial = _initial)
  }
  def apply(): ConsumerOpts = ConsumerOptsImpl(SubscriptionInitialPosition.Latest)
}
