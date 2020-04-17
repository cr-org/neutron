package cr.pulsar

import cats.Show.show
import io.estatico.newtype.macros.newtype

sealed abstract case class Topic(url: String)

object Topic {
  import cats.implicits._

  @newtype case class TopicName(value: String)

  sealed trait Type
  object Type {
    case object Persistent    extends Type
    case object NonPersistent extends Type
    implicit val showInstance = show[Type] {
      case Persistent    => "persistent"
      case NonPersistent => "non-persistent"
    }
  }

  def apply(cfg: Config, topic: TopicName, typ: Type): Topic =
    new Topic(s"${typ.show}://${cfg.tenant.value}/${cfg.namespace.value}/${topic.value}") {}
}
