# Neutron

Neutron is a purely functional [Apache Pulsar](https://pulsar.apache.org/) client for Scala, build on top of [fs2](https://fs2.io) and the Java Pulsar client.

It is published for Scala $scala-versions$ and can be included in your project.

@@dependency[sbt,Maven,Gradle] {
  group="$org$" artifact="$neutron-core$" version="$version$"
  group2="$org$" artifact2="$neutron-function$" version2="$version$"
}

## Quick start

Here's a quick consumer / producer example using Neutron.

```scala mdoc:compile-only
import cats.Inject
import cats.effect._
import cats.implicits._
import fs2.Stream
import cr.pulsar._
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import scala.concurrent.duration._

object Demo extends IOApp {

  // Pulsar configuration
  val config  = Config.default
  val topic   = Topic(config, Topic.Name("my-topic"), Topic.Type.NonPersistent)

  // Consumer details
  val subs    = Subscription(Subscription.Name("my-sub"), Subscription.Type.Shared)
  val initPos = SubscriptionInitialPosition.Latest

  // Producer details
  val shardKey: String => Producer.MessageKey = _ => Producer.MessageKey.Default
  val batching = Producer.Batching.Disabled

  // Needed for consumers and producers to be able to decode and encode messages, respectively
  implicit val stringBytesInject: Inject[String, Array[Byte]] =
    new Inject[String, Array[Byte]] {
      override val inj: String => Array[Byte] = _.getBytes("UTF-8")
      override val prj: Array[Byte] => Option[String] = new String(_, "UTF-8").some
    }

  val resources: Resource[IO, (Consumer[IO], Producer[IO, String])] =
    for {
      client <- PulsarClient.create[IO](config.serviceUrl)
      blocker <- Blocker[IO]
      consumer <- Consumer.create[IO](client, topic, subs, initPos)
      producer <- Producer.create[IO, String](client, topic, shardKey, batching, blocker)
    } yield (consumer, producer)

  def run(args: List[String]): IO[ExitCode] =
    Stream
      .resource(resources)
      .flatMap {
        case (consumer, producer) =>
          val consume =
            consumer
              .subscribe
              .evalTap(m => IO(println(stringBytesInject.prj(m.getData))))
              .evalMap(m => consumer.ack(m.getMessageId))

          val produce =
            Stream
              .emit("test data")
              .covary[IO]
              .metered(3.seconds)
              .evalMap(producer.send_)

          consume.concurrently(produce)
      }.compile.drain.as(ExitCode.Success)

}
```

Another common `Inject` instance we use is an encoder / decoder based on the Circe library:

```scala
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._

implicit def circeBytesInject[T: Encoder: Decoder]: Inject[T, Array[Byte]] =
  new Inject[T, Array[Byte]] {
    val inj: T => Array[Byte] =
      t => t.asJson.noSpaces.getBytes("UTF-8")

    val prj: Array[Byte] => Option[T] =
      bytes => decode[T](new String(bytes, "UTF-8")).toOption
  }
```
