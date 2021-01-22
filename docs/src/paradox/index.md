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

Neutron relies on `cats.Inject[A, Array[Byte]]` instances to be able to decode & encode messages from & to raw bytes instead of using the [native schema solution](https://pulsar.apache.org/docs/en/schema-get-started/). As functional programmers, we believe this has certain benefits. In the example above, we are using a standard "UTF-8" encoding `String <=> Array[Byte]`, brought by `import cr.pulsar.schema.utf8._`.

At Chatroulette, we use JSON-serialised data for which we define an `Inject` instance based on Circe codecs. Those interested in doing the same can leverage the Circe integration by adding the `neutron-circe` extra dependency (available since `v0.0.2`).

Once you added the dependency, you are an import away from having JSON schema based on Circe.

```scala
import cr.pulsar.schema.circe._
```

Be aware that your datatype needs to provide instances of `io.circe.Encoder` and `io.circe.Decoder` for this instance to become available.
