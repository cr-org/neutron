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
import cr.pulsar.schema.utf8._

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

  val resources: Resource[IO, (Consumer[IO, String], Producer[IO, String])] =
    for {
      pulsar   <- Pulsar.make[IO](config.url)
      consumer <- Consumer.make[IO, String](pulsar, topic, subs)
      producer <- Producer.make[IO, String](pulsar, topic)
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

As of version `0.0.6`, Neutron ships with support for [Pulsar Schema](https://pulsar.apache.org/docs/en/schema-get-started/). The simplest way to get started is to use the given UTF-8 encoding, which makes use of the native `Schema.BYTES`.

```scala mdoc:compile-only
import cr.pulsar.schema.Schema
import cr.pulsar.schema.utf8._

val schema = Schema[String] // summon instance
```

This brings into scope an `Schema[String]` instance, required to initialize consumers and producers. There's also a default instance `Schema[A]`, for any `cats.Inject[A, Array[Byte]]` instance (based on `Schema.BYTES` as well).

At Chatroulette, we use JSON-serialised data for which we derive a `Schema.JSON` based on Circe codecs and Avro schemas. Those interested in doing the same can leverage the Circe integration by adding the `neutron-circe` dependency.

ℹ️ When using schemas, prefer to create the producer(s) before the consumer(s) for fail-fast semantics.

We also need instances for Circe's `Decoder` and `Encoder`, and for `JsonSchema`, which expects an Avro schema, used by Pulsar.
Once you have it, you are an import away from having JSON schema support.

```scala mdoc:compile-only
import cr.pulsar.schema.Schema
import cr.pulsar.schema.circe._

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Event(id: Long, name: String)
object Event {
  implicit val jsonEncoder: Encoder[Event] = deriveEncoder
  implicit val jsonDecoder: Decoder[Event] = deriveDecoder

  implicit val jsonSchema: JsonSchema[Event] = JsonSchema.derive
}

val schema = Schema[Event] // summon an instance
```

The `JsonSchema` can be created directly using `JsonSchema.derive[A]`, which uses [avro4s](https://github.com/sksamuel/avro4s) under the hood. In fact, this is the recommended way but if you want to get something quickly up and running, you could also use auto-derivation.

```scala mdoc:compile-only
import cr.pulsar.schema.Schema
import cr.pulsar.schema.circe.auto._

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Foo(tag: String)
object Foo {
  implicit val jsonEncoder: Encoder[Foo] = deriveEncoder
  implicit val jsonDecoder: Decoder[Foo] = deriveDecoder
}

val schema = Schema[Foo] // summon an instance
```

Notice that `avro4s` is marked as `Provided`, meaning you need to explicitly add it to your classpath.

#### Schema Compatibility Check Strategy

Whenever using schemas, make sure you fully understand the different [strategies](https://pulsar.apache.org/docs/en/schema-evolution-compatibility/#schema-compatibility-check-strategy), which only operate at the namespace level (e.g. see how integration tests are set up in the [run.sh](./run.sh) shell script).

For instance, when using the `BACKWARD` mode, a producer and consumer will fail to initialize if the schemas are incompatible, even if your custom JSON decoder can deserialize the previous model, the Pulsar broker doesn't know about it. E.g. say we have this model in our new application.

```scala
case class Event(uuid: UUID, value: String)
```

The generated Avro schema will look as follows.

```json
{
  "type" : "record",
  "name" : "Event",
  "namespace" : "cr.pulsar.domain",
  "fields" : [ {
    "name" : "uuid",
    "type" : {
      "type" : "string",
      "logicalType" : "uuid"
    }
  }, {
    "name" : "value",
    "type" : "string"
  } ]
}
```

And later on, we introduce a breaking change in the model, adding a new **mandatory** field.

```scala
case class Event(uuid: UUID, value: String, code: Int)
```

This will be rejected at runtime, validated by Pulsar Schemas, when using the BACKWARD mode. The only changes allowed in this mode are:

- Add optional fields
- Delete fields

See the generated Avro schema below.

```json
{
  "type" : "record",
  "name" : "Event",
  "namespace" : "cr.pulsar.domain",
  "fields" : [ {
    "name" : "uuid",
    "type" : {
      "type" : "string",
      "logicalType" : "uuid"
    }
  }, {
    "name" : "value",
    "type" : "string"
  }, {
    "name" : "code",
    "type" : "int"
  } ]
}
```

Instead, we should make the new field optional with a default value for this to work.

```scala
case class Event(uuid: UUID, value: String, code: Option[Int] = None)
```

This is now accepted by Pulsar since any previous `Event` still not consumed from a Pulsar topic can still be processed by the new consumers expecting the new schema.

```json
{
  "type" : "record",
  "name" : "Event",
  "namespace" : "cr.pulsar.domain",
  "fields" : [ {
    "name" : "uuid",
    "type" : {
      "type" : "string",
      "logicalType" : "uuid"
    }
  }, {
    "name" : "value",
    "type" : "string"
  }, {
    "name" : "code",
    "type" : [ "null", "int" ],
    "default" : null
  } ]
}
```

See the difference with the previous schema? This one has a `default: null` in addition to the extra `null` type.
