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

import scala.util.matching.Regex

import cr.pulsar.Topic._
import cr.pulsar.data._

abstract class TopicBuilderExtra[I <: Info] { self: TopicBuilder[I] =>
  def withName(name: String): TopicBuilder[I with Info.Name] =
    self.withName(TopicName(name))

  def withNamePattern(regex: Regex): TopicBuilder[I with Info.Pattern] =
    self.withNamePattern(TopicNamePattern(regex))
}
