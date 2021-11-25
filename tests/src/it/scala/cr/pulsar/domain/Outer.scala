package cr.pulsar.domain

import cats.Eq
import cr.pulsar.schema.circe.JsonSchema
import io.circe._
import io.circe.generic.semiauto._

object Outer {
  case class Inner(code: Int)

  implicit val eq: Eq[Inner] = Eq.by(_.code)

  implicit val jsonEncoder: Encoder[Inner] = deriveEncoder
  implicit val jsonDecoder: Decoder[Inner] = deriveDecoder

  implicit val jsonSchema: JsonSchema[Inner] = JsonSchema.derive
}
