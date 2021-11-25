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

import cats.effect._
import cr.pulsar.Pulsar.PulsarURL
import cr.pulsar.domain._
import weaver.IOSuite

trait NeutronSuite extends IOSuite {
  val url = PulsarURL("pulsar://localhost:6650")

  val batch = Producer.Batching.Disabled
  val shard = (_: Event) => ShardKey.Default

  type Res = Pulsar.Underlying
  override def sharedResource: Resource[IO, Res] = Pulsar.make[IO](url)

  def sub(s: String): Subscription =
    Subscription.Builder
      .withName(s)
      .withType(Subscription.Type.Failover)
      .build

}
