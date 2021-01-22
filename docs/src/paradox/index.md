# Neutron

Neutron is a purely functional [Apache Pulsar](https://pulsar.apache.org/) client for Scala, build on top of [fs2](https://fs2.io) and the Java Pulsar client.

It is published for Scala $scala-versions$. You can include it in your project by adding the following dependencies.

@@dependency[sbt,Maven,Gradle] {
  group="$org$" artifact="$neutron-core$" version="$version$"
  group2="$org$" artifact2="$neutron-circe$" version2="$version$"
  group3="$org$" artifact3="$neutron-function$" version3="$version$"
}

## Quick start

Here's a quick consumer / producer example using Neutron. Note: both are fully asynchronous.

```scala mdoc:compile-only
import scala.concurrent.duration._

import cats.effect._
import cats.implicits._
import fs2.Stream

import cr.pulsar._
import cr.pulsar.schema.Schemas

object Demo extends IOApp {

  val config = Config.Builder.default

  val topic  =
    Topic.Builder
      .withName("my-topic")
      .withConfig(config)
      .withType(Topic.Type.NonPersistent)
      .build

  val subs =
    Subscription.Builder
      .withName("my-sub")
      .withType(Subscription.Type.Shared)
      .build

  val schema = Schemas.utf8

  val resources: Resource[IO, (Consumer[IO, String], Producer[IO, String])] =
    for {
      pulsar   <- Pulsar.create[IO](config.url)
      consumer <- Consumer.create[IO, String](pulsar, topic, subs, schema)
      producer <- Producer.create[IO, String](pulsar, topic, schema)
    } yield (consumer, producer)

  def run(args: List[String]): IO[ExitCode] =
    Stream
      .resource(resources)
      .flatMap {
        case (consumer, producer) =>
          val consume =
            consumer
              .subscribe
              .evalMap(m => IO(println(m.payload)) >> consumer.ack(m.id))

          val produce =
            Stream
              .emit("test data")
              .covary[IO]
              .metered(3.seconds)
              .evalMap(producer.send_(_))

          consume.concurrently(produce)
      }.compile.drain.as(ExitCode.Success)

}
```

### Schema

As of version `0.0.5`, Neutron ships with support for [Pulsar Schema](https://pulsar.apache.org/docs/en/schema-get-started/). The simplest way to get started is to use the given UTF-8 encoding, which makes use of the native `Schema.BYTES`.

```scala mdoc:compile-only
import cr.pulsar.schema.Schemas

val schema = Schemas.utf8
```

It is also possible to derive a `Schema.BYTES` for a given `cats.Inject[A, Array[Byte]]` instance.

At Chatroulette, we use JSON-serialised data for which we derive a `Schema.JSON` based on Circe codecs. Those interested in doing the same can leverage the Circe integration by adding the `neutron-circe` dependency.

Once you have it, you are an import away from having JSON schema support.

```scala mdoc:compile-only
import cr.pulsar.schema.circe.JsonSchema

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Event(id: Long, name: String)
object Event {
  implicit val jsonEncoder: Encoder[Event] = deriveEncoder
  implicit val jsonDecoder: Decoder[Event] = deriveDecoder
}

val schema = JsonSchema[Event]
```

Be aware that your datatype needs to provide instances of `io.circe.Encoder` and `io.circe.Decoder` for this instance to become available.
