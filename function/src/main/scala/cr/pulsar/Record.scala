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

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

import org.apache.pulsar.client.api.Message
import org.apache.pulsar.functions.api.{ Record => JavaRecord }

final case class Record[T](private val ctx: JavaRecord[T]) {
  def ack(): Unit  = ctx.ack()
  def fail(): Unit = ctx.fail()

  def destinationTopic: Option[String] = ctx.getDestinationTopic.toScala
  def eventTime: Option[Long]          = ctx.getEventTime.toScala.map(x => x)
  def key: Option[String]              = ctx.getKey.toScala
  def message: Option[Message[T]]      = ctx.getMessage.toScala
  def partitionId: Option[String]      = ctx.getPartitionId.toScala
  def properties: Map[String, String]  = ctx.getProperties.asScala.toMap
  def recordSequence: Option[Long]     = ctx.getRecordSequence.toScala.map(x => x)
  def topicName: Option[String]        = ctx.getTopicName.toScala
  def value: T                         = ctx.getValue
}
