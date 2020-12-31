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

import cats.Inject
import cats.effect.Bracket

package object internal {

  /**
    * A convenient dependent summoner for the Cats Effect hierarchy. Inspired by izumi's BIO.
    * Auto-narrows to the most powerful available class but it does not work below Monad (ambiguous implicits).
    *
    * {{{
    *   import cr.pulsar.internal.summoner.F
    *
    *   def program[F[_]: Sync]: F[Unit] =
    *     F.delay(println("Hello world!"))
    * }}}
    */
  @inline final def F[FR[_]](implicit FR: Bracket[FR, Throwable]): FR.type = FR

  @inline final def E[T](implicit T: Inject[T, Array[Byte]]): T.type = T
}
