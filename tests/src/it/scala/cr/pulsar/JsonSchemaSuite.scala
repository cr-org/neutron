package cr.pulsar

import cats.effect.{ IO, Resource }
import cr.pulsar.Topic.Type
import cr.pulsar.domain.Fruit
import cr.pulsar.domain.Outer.Inner
import cr.pulsar.schema.circe._

object JsonSchemaSuite extends NeutronSuite {
  test("Support for JSONSchema with ADTs") { client =>
    val vTopic = Topic.simple("fruits-adt", Type.Persistent)

    val res: Resource[IO, (Consumer[IO, Fruit], Producer[IO, Fruit])] =
      for {
        producer <- Producer.make[IO, Fruit](client, vTopic)
        consumer <- Consumer.make[IO, Fruit](client, vTopic, sub("fruits"))
      } yield consumer -> producer

    res.use(_ => IO.pure(success))
  }

  test("Support for JSONSchema with class defined within an object") { client =>
    val vTopic = Topic.simple("not-today", Type.Persistent)

    val res: Resource[IO, (Consumer[IO, Inner], Producer[IO, Inner])] =
      for {
        producer <- Producer.make[IO, Inner](client, vTopic)
        consumer <- Consumer.make[IO, Inner](client, vTopic, sub("outer-inner"))
      } yield consumer -> producer

    res.use(_ => IO.pure(success))
  }
}
