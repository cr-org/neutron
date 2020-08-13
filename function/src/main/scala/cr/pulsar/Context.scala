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

import java.nio.ByteBuffer

import cr.pulsar.WindowContext._
import org.apache.pulsar.client.api.{ Schema, TypedMessageBuilder }
import org.apache.pulsar.functions.api.{ Context => JavaContext }
import org.slf4j.Logger

import collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import scala.reflect.ClassTag

final case class Context(private val ctx: JavaContext) {
  def tenant: Tenant                     = Tenant(ctx.getTenant)
  def namespace: Namespace               = Namespace(ctx.getNamespace)
  def functionName: FunctionName         = FunctionName(ctx.getFunctionName)
  def functionId: FunctionId             = FunctionId(ctx.getFunctionId)
  def instanceId: InstanceId             = InstanceId(ctx.getInstanceId)
  def numInstances: NumInstances         = NumInstances(ctx.getNumInstances)
  def functionVersion: FunctionVersion   = FunctionVersion(ctx.getFunctionVersion)
  def inputTopics: Seq[InputTopic]       = ctx.getInputTopics.asScala.toSeq.map(InputTopic(_))
  def outputTopic: OutputTopic           = OutputTopic(ctx.getOutputTopic)
  def outputSchemaType: OutputSchemaType = OutputSchemaType(ctx.getOutputSchemaType)

  def logger: Logger = ctx.getLogger

  def incrCounter(key: String, amount: Long): Unit = ctx.incrCounter(key, amount)
  def getCounter(key: String): Long                = ctx.getCounter(key)

  def putState(key: String, value: ByteBuffer): Unit = ctx.putState(key, value)
  def getState(key: String): Option[ByteBuffer]      = Option(ctx.getState(key))

  def userConfigMap: Map[String, AnyRef] = ctx.getUserConfigMap.asScala.toMap
  def userConfigValue[T: ClassTag](key: String): Option[T] =
    ctx.getUserConfigValue(key).asScala.collect { case x: T => x }

  def userConfigValueOrElse[T: ClassTag](key: String, defaultValue: T): T =
    userConfigValue[T](key).getOrElse(defaultValue)

  def recordMetric(metricName: String, value: Double): Unit =
    ctx.recordMetric(metricName, value)

  def newOutputMessage[O](topicName: String, schema: Schema[O]): TypedMessageBuilder[O] =
    ctx.newOutputMessage(topicName, schema)
}
