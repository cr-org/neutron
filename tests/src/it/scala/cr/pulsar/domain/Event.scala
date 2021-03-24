package cr.pulsar
package domain

import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

import cats.Eq
import io.circe._
import io.circe.generic.semiauto._

case class Event(uuid: UUID, value: String) {
  def shardKey: ShardKey = ShardKey.Of(uuid.toString.getBytes(UTF_8))
  def toV2: Event_V2     = Event_V2(uuid, value, 0)
}

object Event {
  implicit val eq: Eq[Event] = Eq.by(_.uuid)

  implicit val jsonEncoder: Encoder[Event] = deriveEncoder
  implicit val jsonDecoder: Decoder[Event] = deriveDecoder
}
