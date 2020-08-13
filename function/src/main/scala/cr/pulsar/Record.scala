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

import org.apache.pulsar.client.api.{ Message, Schema }
import org.apache.pulsar.functions.api.{ Record => JavaRecord }

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

final case class Record[T](private val ctx: JavaRecord[T]) {
  def ack(): Unit  = ctx.ack()
  def fail(): Unit = ctx.fail()

  def destinationTopic: Option[String] = ctx.getDestinationTopic.asScala
  def eventTime: Option[Long]          = ctx.getEventTime.asScala.map(x => x)
  def key: Option[String]              = ctx.getKey.asScala
  def message: Option[Message[T]]      = ctx.getMessage.asScala
  def partitionId: Option[String]      = ctx.getPartitionId.asScala
  def properties: Map[String, String]  = ctx.getProperties.asScala.toMap
  def recordSequence: Option[Long]     = ctx.getRecordSequence.asScala.map(x => x)
  def schema: Schema[T]                = ctx.getSchema
  def topicName: Option[String]        = ctx.getTopicName.asScala
  def value: T                         = ctx.getValue
}
