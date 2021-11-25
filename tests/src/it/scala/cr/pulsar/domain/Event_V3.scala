package cr.pulsar
package domain

import cats.Eq
import cr.pulsar.schema.circe.JsonSchema
import io.circe._
import io.circe.generic.semiauto._

import java.util.UUID

case class Event_V3(uuid: UUID, value: String, thisFieldBreaksCompat: Int)

object Event_V3 {
  implicit val eq: Eq[Event_V3] = Eq.by(_.uuid)

  implicit val jsonEncoder: Encoder[Event_V3] = deriveEncoder
  implicit val jsonDecoder: Decoder[Event_V3] = deriveDecoder[Event_V3]

  implicit val jsonSchema: JsonSchema[Event_V3] = JsonSchema.derive
}
