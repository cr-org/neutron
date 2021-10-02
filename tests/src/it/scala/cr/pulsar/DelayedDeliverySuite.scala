package cr.pulsar

import cats.effect.{ IO, Ref, Resource }
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe.circeInstance
import weaver.IOSuite

import java.time.{ Duration, Instant }
import java.util.UUID
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

object DelayedDeliverySuite extends IOSuite {

  val cfg = Config.Builder.default

  type Res = Pulsar.T
  override def sharedResource: Resource[IO, Res] = Pulsar.make[IO](cfg.url)

  test("A message is published and consumed after a delay with shared subscription") {
    client =>
      val ddTopic = Topic.Builder
        .withName(s"delayed-delivery-suite-${UUID.randomUUID().toString}")
        .withConfig(cfg)
        .build

      val sharedSubscription =
        Subscription.Builder
          .withName("dd-shared")
          .withType(Subscription.Type.Shared)
          .build

      val failoverSubscription =
        Subscription.Builder
          .withName("dd-failover")
          .withType(Subscription.Type.Failover)
          .build

      val event = Event(UUID.randomUUID(), "I'm delayed!")

      def now: IO[Instant]      = IO(Instant.now)
      val delay: FiniteDuration = 2.seconds

      case class ReceivedMessage(sub: Subscription, event: Event, ts: Instant)

      def sendMessage: IO[Instant] =
        Producer
          .make[IO, Event](client, ddTopic)
          .use(_.sendDelayed_(event, delay) >> now)

      def consumeMessage(
          sub: Subscription,
          ref: Ref[IO, List[ReceivedMessage]]
      ): IO[Unit] =
        Consumer
          .make[IO, Event](client, ddTopic, sub)
          .use {
            _.autoSubscribe
              .evalMap(e => now.map(ts => ReceivedMessage(sub, e, ts)))
              .take(1)
              .evalMap(msg => ref.update(_ :+ msg))
              .compile
              .drain
          }

      for {
        ref <- Ref.of[IO, List[ReceivedMessage]](List.empty)

        start <- consumeMessage(failoverSubscription, ref) &>
                    consumeMessage(sharedSubscription, ref) &>
                    sendMessage

        result <- ref.get
      } yield {
        val sharedSubResult = result.exists { r =>
          Duration
            .between(start, r.ts)
            .toMillis > 2000 && r.sub.`type` == Subscription.Type.Shared
        }

        val failoverSubResult = result.exists { r =>
          Duration
            .between(start, r.ts)
            .toMillis < 2000 && r.sub.`type` == Subscription.Type.Failover
        }

        assert(sharedSubResult) && assert(failoverSubResult)
      }
  }
}
