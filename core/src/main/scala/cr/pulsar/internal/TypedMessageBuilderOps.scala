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

import cr.pulsar.{ MessageKey, ShardKey }
import org.apache.pulsar.client.api.TypedMessageBuilder

private[pulsar] object TypedMessageBuilderOps {
  implicit class TMBOps[A](val value: TypedMessageBuilder[A]) extends AnyVal {
    def withShardKey(shardKey: ShardKey): TypedMessageBuilder[A] =
      shardKey match {
        case ShardKey.Of(k)   => value.orderingKey(k)
        case ShardKey.Default => value
      }

    def withMessageKey(msgKey: MessageKey): TypedMessageBuilder[A] =
      msgKey match {
        case MessageKey.Of(k) => value.key(k)
        case MessageKey.Empty => value
      }
  }
}
