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

import cats._
import cats.effect._
import cats.effect.concurrent.Deferred
import cats.implicits._
import cr.pulsar.Config._
import java.util.UUID
import munit.FunSuite
import scala.concurrent.ExecutionContext
import scala.util.Try

abstract class PulsarSuite extends FunSuite {

  implicit val `⏳` = IO.contextShift(ExecutionContext.global)
  implicit val `⏰` = IO.timer(ExecutionContext.global)

  private[this] var client: Client.T = null
  private[this] var close: IO[Unit]        = null
  private[this] val latch                  = Deferred[IO, Unit].unsafeRunSync()

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform("IO", {
          case ioa: IO[_] => IO.suspend(ioa).unsafeToFuture
        })

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (cli, release) =
      Client.create[IO](cfg.serviceUrl).allocated.unsafeRunSync()
    this.client = cli
    this.close = release
    latch.complete(()).unsafeRunSync()
  }

  override def afterAll(): Unit = {
    close.unsafeRunSync()
    super.afterAll()
  }

  def withPulsarClient(f: (=> Client.T) => Unit): Unit =
    f {
      //to ensure the resource has been allocated before any test(...) call
      latch.get.unsafeRunSync
      client
    }

  case class Event(uuid: UUID, value: String) {
    def shardKey: Producer.MessageKey =
      Producer.MessageKey.Of(uuid.toString)
  }

  object Event {
    implicit val eq: Eq[Event] = Eq.by(_.uuid)

    implicit val inject: Inject[Event, Array[Byte]] =
      new Inject[Event, Array[Byte]] {
        def inj: Event => Array[Byte] =
          e => s"${e.uuid.toString}".getBytes("UTF-8")
        def prj: Array[Byte] => Option[Event] =
          bs =>
            Try(UUID.fromString(new String(bs, "UTF-8"))).toOption.map { i =>
              Event(i, "foo")
            }
      }
  }

  lazy val cfg = Config(
    PulsarTenant("public"),
    PulsarNamespace("default"),
    PulsarURL("pulsar://localhost:6650")
  )

}
