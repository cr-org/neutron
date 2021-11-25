package cr.pulsar
package domain

import cats.Eq
import cr.pulsar.schema.circe.JsonSchema
import io.circe._
import io.circe.generic.semiauto._

import java.util.UUID

case class Event_V2(uuid: UUID, value: String, code: Option[Int] = None)

object Event_V2 {
  implicit val eq: Eq[Event_V2] = Eq.by(_.uuid)

  implicit val jsonEncoder: Encoder[Event_V2] = deriveEncoder
  implicit val jsonDecoder: Decoder[Event_V2] = deriveDecoder[Event_V2]

  implicit val jsonSchema: JsonSchema[Event_V2] = JsonSchema.derive
}
