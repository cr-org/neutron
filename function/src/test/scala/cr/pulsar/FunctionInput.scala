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
import java.{ lang, util }
import java.util.Optional
import java.util.concurrent.CompletableFuture

import scala.jdk.CollectionConverters._

import org.apache.pulsar.client.api.{ ConsumerBuilder, Schema, TypedMessageBuilder }
import org.apache.pulsar.functions.api.{
  Context => JavaContext,
  Record => JavaRecord,
  WindowContext => JavaWindowContext
}
import org.slf4j.Logger

object FunctionInput {
  def emptyWindowCtx: JavaWindowContext = new JavaWindowContext {
    override def getTenant: String                                 = ???
    override def getNamespace: String                              = ???
    override def getFunctionName: String                           = ???
    override def getFunctionId: String                             = ???
    override def getInstanceId: Int                                = ???
    override def getNumInstances: Int                              = ???
    override def getFunctionVersion: String                        = ???
    override def getInputTopics: util.Collection[String]           = ???
    override def getOutputTopic: String                            = ???
    override def getOutputSchemaType: String                       = ???
    override def getLogger: Logger                                 = ???
    override def incrCounter(key: String, amount: Long): Unit      = ???
    override def getCounter(key: String): Long                     = ???
    override def putState(key: String, value: ByteBuffer): Unit    = ???
    override def getState(key: String): ByteBuffer                 = ???
    override def getUserConfigMap: util.Map[String, AnyRef]        = ???
    override def getUserConfigValue(key: String): Optional[AnyRef] = ???
    override def getUserConfigValueOrDefault(key: String, defaultValue: Any): AnyRef =
      ???
    override def recordMetric(metricName: String, value: Double): Unit = ???
    override def publish[O](
        topicName: String,
        `object`: O,
        schemaOrSerdeClassName: String
    ): CompletableFuture[Void] = ???
    override def publish[O](topicName: String, `object`: O): CompletableFuture[Void] =
      ???
  }

  def emptyCtx: JavaContext = new JavaContext {
    override def getTenant: String                                 = ???
    override def getNamespace: String                              = ???
    override def getFunctionName: String                           = ???
    override def getFunctionId: String                             = ???
    override def getInstanceId: Int                                = ???
    override def getNumInstances: Int                              = ???
    override def getFunctionVersion: String                        = ???
    override def getInputTopics: util.Collection[String]           = ???
    override def getOutputTopic: String                            = ???
    override def getOutputSchemaType: String                       = ???
    override def getLogger: Logger                                 = ???
    override def incrCounter(key: String, amount: Long): Unit      = ???
    override def getCounter(key: String): Long                     = ???
    override def putState(key: String, value: ByteBuffer): Unit    = ???
    override def getState(key: String): ByteBuffer                 = ???
    override def getUserConfigMap: util.Map[String, AnyRef]        = ???
    override def getUserConfigValue(key: String): Optional[AnyRef] = ???
    override def getUserConfigValueOrDefault(key: String, defaultValue: Any): AnyRef =
      ???
    override def recordMetric(metricName: String, value: Double): Unit = ???
    override def publish[O](
        topicName: String,
        `object`: O,
        schemaOrSerdeClassName: String
    ): CompletableFuture[Void] = ???
    override def publish[O](topicName: String, `object`: O): CompletableFuture[Void] =
      ???
    override def getCurrentRecord: JavaRecord[_] = ???
    override def incrCounterAsync(key: String, amount: Long): CompletableFuture[Void] =
      ???
    override def getCounterAsync(key: String): CompletableFuture[lang.Long] = ???
    override def putStateAsync(key: String, value: ByteBuffer): CompletableFuture[Void] =
      ???
    override def deleteState(key: String): Unit                            = ???
    override def deleteStateAsync(key: String): CompletableFuture[Void]    = ???
    override def getStateAsync(key: String): CompletableFuture[ByteBuffer] = ???
    override def getSecret(secretName: String): String                     = ???
    override def newOutputMessage[O](
        topicName: String,
        schema: Schema[O]
    ): TypedMessageBuilder[O]                                                 = ???
    override def newConsumerBuilder[O](schema: Schema[O]): ConsumerBuilder[O] = ???
  }

  def input[A](seq: Seq[A]): util.Collection[JavaRecord[A]] = {
    val records = seq.map { x =>
      new JavaRecord[A] { override def getValue: A = x }
    }

    records.asJavaCollection
  }
}
