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
import cats.syntax.all._
import cats.effect.implicits._
import java.util.concurrent._

object FutureLift {

  private[internal] type JFuture[A] = CompletionStage[A] with Future[A]

  implicit class CompletableFutureLift[F[_]: Async: Spawn, A](
      fa: F[CompletableFuture[A]]
  ) {
    def futureLift: F[A] = liftJFuture[F, CompletableFuture[A], A](fa)
  }

  private[internal] def liftJFuture[
      F[_]: Async: Spawn,
      G <: JFuture[A],
      A
  ](fa: F[G]): F[A] = {
    val lifted: F[A] =
      fa.flatMap { f =>
        F.async { cb =>
          f.handle[Unit] { (res: A, err: Throwable) =>
            err match {
              case null =>
                cb(Right(res))
              case _: CancellationException =>
                ()
              case ex: CompletionException if ex.getCause ne null =>
                cb(Left(ex.getCause))
              case ex =>
                cb(Left(ex))
            }
          }
          F.delay(Option(F.delay(f.cancel(true)).void))
        }
      }
    lifted.guarantee(F.cede)
  }

}
