package cr.pulsar.internal

import cats.effect.{Async, ContextShift}
import cats.implicits._
import cats.effect.implicits._
import java.util.concurrent.{CompletableFuture, CompletionStage, Future}

object FutureLift {

  private[internal] type JFuture[A] = CompletionStage[A] with Future[A]

  implicit class CompletableFutureLift[F[_]: Async: ContextShift, A](
      fa: F[CompletableFuture[A]]
  ) {
    def futureLift: F[A] = liftJFuture[F, CompletableFuture[A], A](fa)
  }

  private[internal] def liftJFuture[
      F[_]: Async: ContextShift,
      G <: JFuture[A],
      A
  ](fa: F[G]): F[A] =
    fa.flatMap { f =>
      F.async[A] { cb =>
          f.handle[Unit] { (value: A, t: Throwable) =>
            if (t != null) cb(Left(t))
            else cb(Right(value))
          }
          ()
        }
        .guarantee(F.shift)
    }

}
