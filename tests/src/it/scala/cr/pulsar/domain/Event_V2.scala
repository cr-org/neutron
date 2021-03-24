package cr.pulsar
package domain

import java.util.UUID

import cats.Eq
import io.circe._
import io.circe.generic.semiauto._

case class Event_V2(uuid: UUID, value: String, code: Int)

object Event_V2 {
  implicit val eq: Eq[Event_V2] = Eq.by(_.uuid)

  implicit val jsonEncoder: Encoder[Event_V2] = deriveEncoder
  implicit val jsonDecoder: Decoder[Event_V2] =
    deriveDecoder[Event_V2] or Event.jsonDecoder.map(_.toV2)
}
