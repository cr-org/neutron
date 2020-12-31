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

import scala.annotation.implicitNotFound
import scala.util.matching.Regex

import cr.pulsar.data._

import cats.Show
import cats.syntax.all._

sealed abstract class Topic {
  val name: TopicName
  val url: TopicURL
}

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

  implicit val showTopic: Show[Topic] =
    Show[String].contramap(_.url.value)

  sealed trait Type
  object Type {
    case object Persistent extends Type
    case object NonPersistent extends Type
    implicit val showType: Show[Type] = Show.show[Type] {
      case Persistent    => "persistent"
      case NonPersistent => "non-persistent"
    }
  }

  private def buildUrl(cfg: Config, name: TopicName, `type`: Type): TopicURL =
    TopicURL(s"${`type`.show}://${cfg.tenant.value}/${cfg.namespace.value}/${name.value}")

  sealed abstract class Pattern {
    val url: TopicURL
  }

  private def buildRegexUrl(
      cfg: Config,
      namePattern: TopicNamePattern,
      `type`: Type
  ): TopicURL =
    TopicURL(
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

    type Mandatory        = Empty with Config with Name with Type
    type PatternMandatory = Empty with Config with Pattern with Type
  }

  case class TopicBuilder[I <: Info] protected (
      _name: TopicName = TopicName(""),
      _pattern: TopicNamePattern = TopicNamePattern("".r),
      _config: Config = Config.Builder.default,
      _type: Type = Type.Persistent
  ) {
    def withName(name: TopicName): TopicBuilder[I with Info.Name] =
      this.copy(_name = name)

    def withName(name: String): TopicBuilder[I with Info.Name] =
      withName(TopicName(name))

    def withNamePattern(pattern: TopicNamePattern): TopicBuilder[I with Info.Pattern] =
      this.copy(_pattern = pattern)

    def withNamePattern(regex: Regex): TopicBuilder[I with Info.Pattern] =
      withNamePattern(TopicNamePattern(regex))

    def withConfig(config: Config): TopicBuilder[I with Info.Config] =
      this.copy(_config = config)

    def withType(typ: Type): TopicBuilder[I with Info.Type] =
      this.copy(_type = typ)

    /**
      * It creates a topic. By default, Type=Persistent.
      */
    def build(
        implicit @implicitNotFound(
          "Topic.Name and Config are mandatory. By default Type=Persistent."
        ) ev: I =:= Info.Mandatory
    ): Topic =
      new Topic {
        val name = _name
        val url  = buildUrl(_config, _name, _type)
      }

    /**
      * It creates a topic for a regex pattern. By default, Type=Persistent.
      */
    def buildPattern(
        implicit @implicitNotFound(
          "Topic.NamePattern and Config are mandatory. By default Type=Persistent."
        ) ev: I =:= Info.PatternMandatory
    ): Topic.Pattern =
      new Topic.Pattern {
        val url = buildRegexUrl(_config, _pattern, _type)
      }

  }

  object Builder extends TopicBuilder[Info.Empty with Info.Type]()

}
