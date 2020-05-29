package cr.pulsar

import cats.Show.show
import io.estatico.newtype.macros.newtype

sealed abstract case class Topic(name: Topic.Name, url: Topic.URL)

/**
  * Topic names are URLs that have a well-defined structure:
  *
  * {{{
  * {persistent|non-persistent}://tenant/namespace/topic
  * }}}
  *
  * Find out more at [[https://pulsar.apache.org/docs/en/concepts-messaging/#topics]]
  */
object Topic {
  import cats.implicits._

  @newtype case class Name(value: String)
  @newtype case class URL(value: String)

  sealed trait Type
  object Type {
    case object Persistent extends Type
    case object NonPersistent extends Type
    implicit val showInstance = show[Type] {
      case Persistent    => "persistent"
      case NonPersistent => "non-persistent"
    }
  }

  def apply(cfg: Config, name: Topic.Name, typ: Type): Topic =
    new Topic(
      name,
      URL(s"${typ.show}://${cfg.tenant.value}/${cfg.namespace.value}/${name.value}")
    ) {}
}
