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

import cats.Show.show
import io.estatico.newtype.macros.newtype
import scala.util.matching.Regex

// Builder-style abstract class instead of case class to allow for bincompat-friendly extension in future versions.
sealed abstract class Topic {
  val name: Topic.Name
  val url: Topic.URL
  def withType(_type: Topic.Type): Topic
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
  import cats.syntax.all._

  @newtype case class Name(value: String)
  @newtype case class NamePattern(value: Regex)
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

  private def buildUrl(cfg: Config, name: Name, `type`: Type): URL =
    URL(s"${`type`.show}://${cfg.tenant.value}/${cfg.namespace.value}/${name.value}")

  private case class TopicImpl(
      name: Name,
      url: URL,
      cfg: Config
  ) extends Topic {
    def withType(_type: Type): Topic =
      copy(url = buildUrl(cfg, name, _type))
  }

  /**
    * It creates a topic with default configuration.
    *
    * - type: Persistent
    */
  def apply(name: String, cfg: Config): Topic = {
    val _name = Name(name)
    TopicImpl(_name, buildUrl(cfg, _name, Type.Persistent), cfg)
  }

  // ------- Pattern topic -------

  sealed abstract class Pattern {
    val url: URL
    def withType(_type: Type): Pattern
  }

  private def buildRegexUrl(cfg: Config, namePattern: NamePattern, `type`: Type): URL =
    URL(
      s"${`type`.show}://${cfg.tenant.value}/${cfg.namespace.value}/${namePattern.value.regex}"
    )

  private case class PatternImpl(
      namePattern: NamePattern,
      url: URL,
      cfg: Config
  ) extends Pattern {
    def withType(_type: Type): Pattern =
      copy(url = buildRegexUrl(cfg, namePattern, _type))
  }

  /**
    * It creates a topic for a regex pattern with default configuration.
    *
    * - type: Persistent
    */
  def pattern(regex: Regex, cfg: Config): Topic.Pattern = {
    val np = NamePattern(regex)
    PatternImpl(np, buildRegexUrl(cfg, np, Type.Persistent), cfg)
  }

}
