/*
 * Copyright 2020 Chatroulette
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cr.pulsar

import cats.Show
import cats.syntax.all._
import io.estatico.newtype.macros.newtype
import scala.annotation.implicitNotFound
import scala.util.matching.Regex

sealed trait Topic

/**
  * Topic names are URLs that have a well-defined structure:
  *
  * {{{
  * {persistent|non-persistent}://tenant/namespace/topic
  * }}}
  *
  * It could be either `Single` for one or `Multi` (taking a regular expression) for
  * consuming from multiple topics.
  *
  * Find out more at [[https://pulsar.apache.org/docs/en/concepts-messaging/#topics]]
  */
object Topic {

  sealed abstract class Single extends Topic {
    val name: Topic.Name
    val url: Topic.URL
  }

  sealed abstract class Multi extends Topic {
    val url: URL
  }

  implicit val showTopic: Show[Topic] =
    Show[String].contramap {
      case s: Single => s.url.value
      case m: Multi  => m.url.value
    }

  @newtype case class Name(value: String)
  @newtype case class NamePattern(value: Regex)
  @newtype case class URL(value: String)

  sealed trait Type
  object Type {
    case object Persistent extends Type
    case object NonPersistent extends Type
    implicit val showType = Show.show[Type] {
      case Persistent    => "persistent"
      case NonPersistent => "non-persistent"
    }
  }

  private def buildUrl(cfg: Config, name: Name, `type`: Type): URL =
    URL(s"${`type`.show}://${cfg.tenant.value}/${cfg.namespace.value}/${name.value}")

  private def buildRegexUrl(cfg: Config, namePattern: NamePattern, `type`: Type): URL =
    URL(
      s"${`type`.show}://${cfg.tenant.value}/${cfg.namespace.value}/${namePattern.value.regex}"
    )

  /**************** Type-level builder ******************/
  sealed trait Info
  object Info {
    sealed trait Empty extends Info
    sealed trait Config extends Info
    sealed trait Name extends Info
    sealed trait Pattern extends Info
    sealed trait Type extends Info

    type SingleMandatory = Empty with Config with Name with Type
    type MultiMandatory  = Empty with Config with Pattern with Type
  }

  case class TopicBuilder[I <: Info] protected (
      _name: Either[Name, NamePattern] = Name("").asLeft,
      _config: Config = Config.Builder.default,
      _type: Type = Type.Persistent
  ) {
    def withName(name: Name): TopicBuilder[I with Info.Name] =
      this.copy(_name = name.asLeft)

    def withName(name: String): TopicBuilder[I with Info.Name] =
      withName(Name(name))

    def withNamePattern(pattern: NamePattern): TopicBuilder[I with Info.Pattern] =
      this.copy(_name = pattern.asRight)

    def withNamePattern(regex: Regex): TopicBuilder[I with Info.Pattern] =
      withNamePattern(NamePattern(regex))

    def withConfig(config: Config): TopicBuilder[I with Info.Config] =
      this.copy(_config = config)

    def withType(typ: Type): TopicBuilder[I with Info.Type] =
      this.copy(_type = typ)

    /**
      * It creates a topic of type Single. By default, Type=Persistent.
      */
    def build(
        implicit @implicitNotFound(
          "Topic.Name or Topic.Pattern, and Config are mandatory. By default Type=Persistent."
        ) ev: I =:= Info.SingleMandatory
    ): Topic.Single = {
      val t = _name.swap.toOption.get
      new Single {
        val name = t
        val url  = buildUrl(_config, t, _type)
      }
    }

    /**
      * It creates a topic of type Multi. By default, Type=Persistent.
      */
    def buildMulti(
        implicit @implicitNotFound(
          "Topic.Pattern, and Config are mandatory. By default Type=Persistent."
        ) ev: I =:= Info.MultiMandatory
    ): Topic.Multi = {
      val m = _name.toOption.get
      new Multi {
        val url = buildRegexUrl(_config, m, _type)
      }
    }

  }

  object Builder extends TopicBuilder[Info.Empty with Info.Type]()

}
