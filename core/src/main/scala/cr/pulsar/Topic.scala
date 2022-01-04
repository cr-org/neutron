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

import scala.util.matching.Regex

sealed trait Topic {
  def url: Topic.URL
}

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

  case class Single(url: Topic.URL) extends Topic
  case class Multi(url: Topic.URL) extends Topic

  implicit val showTopic: Show[Topic] =
    Show[String].contramap {
      case s: Single => s.url.value
      case m: Multi  => m.url.value
    }

  @newtype case class Tenant(value: String)
  object Tenant {
    val default: Tenant = Tenant("public")
  }

  @newtype case class Namespace(value: String)
  object Namespace {
    val default: Namespace = Namespace("default")
  }

  @newtype case class Name(value: String)
  @newtype case class NamePattern(value: Regex)
  @newtype case class URL(value: String)

  sealed trait Type
  object Type {
    case object Persistent extends Type
    case object NonPersistent extends Type
    implicit val showType: Show[Type] = Show.show[Type] {
      case Persistent    => "persistent"
      case NonPersistent => "non-persistent"
    }
  }

  private def buildUrl(tenant: Tenant, namespace: Namespace, name: Name, `type`: Type): URL =
    URL(s"${`type`.show}://${tenant.value}/${namespace.value}/${name.value}")

  private def buildRegexUrl(tenant: Tenant, namespace: Namespace, namePattern: NamePattern, `type`: Type): URL =
    URL(s"${`type`.show}://${tenant.value}/${namespace.value}/${namePattern.value.regex}")

  /**
    * Simple typed constructor to build a topic in `public/default` tenant/namespace
    */
  def simple(name: Name, `type`: Type): Topic.Single =
    Single(buildUrl(Tenant("public"), Namespace("default"), name, `type`))

  /**
    * Simple untyped constructor to build a topic in `public/default` tenant/namespace
    */
  def simple(name: String, `type`: Type): Topic.Single =
    Single(buildUrl(Tenant("public"), Namespace("default"), Name(name), `type`))

  /**
    * Simple typed constructor to build a multi topic in `public/default` tenant/namespace
    */
  def simpleMulti(pattern: NamePattern, `type`: Type): Topic.Multi =
    Multi(buildRegexUrl(Tenant.default, Namespace.default, pattern, `type`))

  /**
    * Simple typed constructor to build a multi topic in public/default tenant/namespace
    */
  def simpleMulti(pattern: Regex, `type`: Type): Topic.Multi =
    Multi(buildRegexUrl(Tenant.default, Namespace.default, NamePattern(pattern), `type`))

  /**
    * Typed constructor to build a topic in a custom tenant/namespace
    */
  def single(tenant: Tenant, namespace: Namespace, name: Name, `type`: Type): Topic.Single =
    Single(buildUrl(tenant, namespace, name, `type`))

  /**
    * Untyped constructor to build a topic in a custom tenant/namespace
    */
  def single(tenant: String, namespace: String, name: String, `type`: Type): Topic.Single =
    Single(buildUrl(Tenant(tenant), Namespace(namespace), Name(name), `type`))

  /**
    * Typed constructor to build a multi topic in a custom tenant/namespace
    */
  def multi(tenant: Tenant, namespace: Namespace, name: Name, `type`: Type): Topic.Multi =
    Multi(buildUrl(tenant, namespace, name, `type`))

}
