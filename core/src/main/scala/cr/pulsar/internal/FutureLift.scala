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

package cr.pulsar.internal

import cats.effect._

import java.util.concurrent._

trait FutureLift[F[_]] {
  def futureLift[A](fa: => CompletableFuture[A]): F[A]
}

object FutureLift {
  implicit def instance[F[_]: Async]: FutureLift[F] = new FutureLift[F] {
    override def futureLift[A](fa: => CompletableFuture[A]): F[A] =
      F.fromCompletableFuture(F.delay(fa))
  }

  def apply[F[_]](implicit fl: FutureLift[F]): FutureLift[F] = fl
}
