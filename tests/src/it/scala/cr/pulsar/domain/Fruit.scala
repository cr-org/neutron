package cr.pulsar.domain

import cats.Eq
import io.circe._
import io.circe.generic.semiauto._

// simple ADT test for JSON schema support
sealed trait Fruit
object Fruit {
  final case class Banana(origin: String) extends Fruit
  final case class Strawberry(_type: String) extends Fruit
  case object Kiwi extends Fruit

  implicit val eq: Eq[Fruit] = Eq.fromUniversalEquals

  implicit val jsonEncoder: Encoder[Fruit] = deriveEncoder
  implicit val jsonDecoder: Decoder[Fruit] = deriveDecoder
}
