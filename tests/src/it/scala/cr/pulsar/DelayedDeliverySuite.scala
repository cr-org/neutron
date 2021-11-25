package cr.pulsar

import cats.effect.{ IO, Ref }
import cr.pulsar.Topic.Type
import cr.pulsar.domain.Event
import cr.pulsar.schema.circe.circeInstance

import java.time.{ Duration, Instant }
import java.util.UUID
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

object DelayedDeliverySuite extends NeutronSuite {
  case class ReceivedMessage(event: Event, ts: Instant)

  val delay: FiniteDuration = 2.seconds
  def now: IO[Instant]      = IO(Instant.now)

  val event: Event        = Event(UUID.randomUUID(), "I'm delayed!")
  val topic: Topic.Single = Topic.simple(s"delayed-delivery-suite-${UUID.randomUUID().toString}", Type.Persistent)

  val subscription: Subscription =
    Subscription.Builder
      .withName(s"dd-shared-${UUID.randomUUID().toString}")
      .withType(Subscription.Type.Shared)
      .build

  test(s"A message is published and consumed after a delay with shared subscription") { client =>
    val resources = for {
      producer <- Producer.make[IO, Event](client, topic)
      consumer <- Consumer.make[IO, Event](client, topic, subscription)
    } yield producer -> consumer

    for {
      start <- now
      ref <- Ref.of[IO, List[ReceivedMessage]](List.empty)

      _ <- resources.use {
            case (producer, consumer) =>
              consumer.autoSubscribe
                .evalMap(e => now.map(ts => ReceivedMessage(e, ts)))
                .evalMap(msg => ref.update(_ :+ msg))
                .take(1)
                .compile
                .drain &> producer.sendDelayed_(event, delay)
          }

      result <- ref.get
    } yield assert(
      result.exists(r => Duration.between(start, r.ts).toMillis >= delay.toMillis)
    )
  }
}
