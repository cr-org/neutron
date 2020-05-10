package cr.pulsar.internal

import cats.effect._
import cats.implicits._
import cats.effect.implicits._
import java.util.concurrent._

object FutureLift {

  private[internal] type JFuture[A] = CompletionStage[A] with Future[A]

  implicit class CompletableFutureLift[F[_]: Concurrent: ContextShift, A](
      fa: F[CompletableFuture[A]]
  ) {
    def futureLift: F[A] = liftJFuture[F, CompletableFuture[A], A](fa)
  }

  private[internal] def liftJFuture[
      F[_]: Concurrent: ContextShift,
      G <: JFuture[A],
      A
  ](fa: F[G]): F[A] = {
    val lifted: F[A] =
      fa.flatMap { f =>
        F.cancelable { cb =>
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
          F.delay(f.cancel(true)).void
        }
      }
    lifted.guarantee(F.shift)
  }

}
