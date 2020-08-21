# neutron

[![CI Status](https://github.com/cr-org/neutron/workflows/Scala/badge.svg)](https://github.com/cr-org/neutron/actions)
[![MergifyStatus](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/cr-org/neutron&style=flat)](https://mergify.io)
[![Maven Central](https://img.shields.io/maven-central/v/com.chatroulette/neutron-core_2.13.svg)](https://search.maven.org/search?q=com.chatroulette.neutron)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

A *pulsar* is a celestial object, thought to be a rapidly rotating neutron star, that emits regular pulses of radio waves and other electromagnetic radiation at rates of up to one thousand pulses per second.

[![pulsar](https://www.jpl.nasa.gov/spaceimages/images/largesize/PIA18845_hires.jpg "An accreting pulsar. Credit NASA/JPL-Caltech")](https://www.jpl.nasa.gov/spaceimages/?search=pulsar&category=#submit)

### Disclaimer

**ChatRoulette** uses this library in production as the base of the whole platform. However, this library is in active development so if you want to use it, **do it at your own risk**.

### Quick start

Here's a quick consumer / producer example.

```scala
import cats.effect._
import cats.implicits._
import fs2._
import cr.pulsar._

object Demo extends IOApp {

  // Pulsar configuration
  val config  = Config.default
  val topic   = Topic(config, Topic.Name("my-topic"), Topic.Type.NonPersistent)

  // Consumer details
  val subs    = Subscription(Subscription.Name("my-sub"), Subscription.Type.Shared)
  val initPos = SubscriptionInitialPosition.Latest

  // Producer details
  val shardKey = _ => Producer.MessageKey.Default
  val batching = Producer.Batching.Disabled

  // Needed for consumers and producers to be able to decode and encode messages, respectively
  implicit val stringBytesInject: Inject[String, Array[Byte]] =
    new Inject[String, Array[Byte]] {
      override val inj: String => Array[Byte] = _.getBytes("UTF-8")
      override val prj: Array[Byte] => Option[String] = new String(_, "UTF-8").some
    }

  val resources: Resource[IO, (Consumer[IO], Producer[IO])] =
    for {
      client <- PulsarClient.create[IO](config.serviceUrl)
      blocker <- Blocker[IO]
      consumer <- Consumer.create[IO](client, topic, subs, initPos)
      producer <- Producer.create[IO, Event](client, topic, shardKey, batching, blocker)
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
              .metered(3.seconds)
              .evalMap(producer.send_)

          consume.concurrently(produce)
      }

}
```

If you are using `sbt`, just add it as one of your dependencies:

```
libraryDependencies += "com.chatroulette" %% "neutron-core" % Version
libraryDependencies += "com.chatroulette" %% "neutron-function" % Version
```

> Check the "Maven Central" badge above to find out the latest version.


Another common `Inject` instance we use is an encoder / decoder based on the Circe library:

```scala
implicit def circeBytesInject[T: Encoder: Decoder]: Inject[T, Array[Byte]] =
  new Inject[T, Array[Byte]] {
    override val inj: T => Array[Byte] =
      t => t.asJson.noSpaces.getBytes(UTF_8)

    override val prj: Array[Byte] => Option[T] =
      bytes => decode[T](new String(bytes, UTF_8)).toOption
  }
```

### Pulsar version

At the moment, we only target Apache Pulsar 2.5.x but we plan to add support for 2.6.x in the near future.

### Build

If you have `sbt` installed, you don't have to worry about anything. Simply run `sbt +test` command in the project root to run the tests.

If you are a `nix` user, make sure you enter a `Nix Shell` by running `nix-shell` at the project's root.

```
sbt +test
```

### Update CI build YAML

To update CI build you should have `dhall-json` installed or simply use `nix-shell` and you'll have it available. Using [Github Actions Dhall](https://github.com/regadas/github-actions-dhall).

```
cd .github/workflows
dhall-to-yaml --file scala.dhall > scala.yml
```
