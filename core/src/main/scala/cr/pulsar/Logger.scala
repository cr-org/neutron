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

import cats.Applicative

trait Logger[F[_], E] {
  def log(topic: Topic.URL, e: E): F[Unit]
}

object Logger {
  def noop[F[_]: Applicative, E]: Logger[F, E] = new Logger[F, E] {
    override def log(topic: Topic.URL, e: E): F[Unit] = F.unit
  }
}
