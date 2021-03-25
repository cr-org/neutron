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
import java.util
import java.util.Optional
import java.util.concurrent.CompletableFuture

import cats.syntax.all._
import cr.pulsar.WindowContext.OutputTopic
import org.apache.pulsar.functions.api.{ WindowContext => JavaWindowContext }
import org.slf4j.Logger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.collection.mutable
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.compat.java8.FutureConverters._

object WindowContextSuite extends SimpleIOSuite with Checkers {
  test("WindowContext mapping of fields is correct") {
    forall {
      (
          t_ns: (String, String),
          fn: String,
          fid: String,
          iid: Int,
          ni: Int,
          fv: String
      ) =>
        val t  = t_ns._1
        val ns = t_ns._2

        val javaCtx = new JavaWindowContext {
          override def getTenant: String                                 = t
          override def getNamespace: String                              = ns
          override def getFunctionName: String                           = fn
          override def getFunctionId: String                             = fid
          override def getInstanceId: Int                                = iid
          override def getNumInstances: Int                              = ni
          override def getFunctionVersion: String                        = fv
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
          override def getUserConfigValueOrDefault(
              key: String,
              defaultValue: Any
          ): AnyRef                                                          = ???
          override def recordMetric(metricName: String, value: Double): Unit = ???
          override def publish[O](
              topicName: String,
              `object`: O,
              schemaOrSerdeClassName: String
          ): CompletableFuture[Void] = ???
          override def publish[O](
              topicName: String,
              `object`: O
          ): CompletableFuture[Void] =
            ???
        }

        val ctx = new WindowContext(javaCtx)

        expect.all(
          ctx.tenant.value === t,
          ctx.namespace.value === ns,
          ctx.functionName.value === fn,
          ctx.functionId.value === fid,
          ctx.instanceId.value === iid,
          ctx.numInstances.value === ni,
          ctx.functionVersion.value === fv
        )
    }
  }

  test("WindowContext mapping of in and out topics is correct") {
    forall {
      (
          it1: String,
          it2: String,
          ot: String,
          ost: String
      ) =>
        val javaCtx = new JavaWindowContext {
          override def getTenant: String          = ???
          override def getNamespace: String       = ???
          override def getFunctionName: String    = ???
          override def getFunctionId: String      = ???
          override def getInstanceId: Int         = ???
          override def getNumInstances: Int       = ???
          override def getFunctionVersion: String = ???
          override def getInputTopics: util.Collection[String] = {
            val arr = new util.ArrayList[String](1)
            arr.add(it1)
            arr.add(it2)
            arr
          }
          override def getOutputTopic: String                            = ot
          override def getOutputSchemaType: String                       = ost
          override def getLogger: Logger                                 = ???
          override def incrCounter(key: String, amount: Long): Unit      = ???
          override def getCounter(key: String): Long                     = ???
          override def putState(key: String, value: ByteBuffer): Unit    = ???
          override def getState(key: String): ByteBuffer                 = ???
          override def getUserConfigMap: util.Map[String, AnyRef]        = ???
          override def getUserConfigValue(key: String): Optional[AnyRef] = ???
          override def getUserConfigValueOrDefault(
              key: String,
              defaultValue: Any
          ): AnyRef                                                          = ???
          override def recordMetric(metricName: String, value: Double): Unit = ???
          override def publish[O](
              topicName: String,
              `object`: O,
              schemaOrSerdeClassName: String
          ): CompletableFuture[Void] = ???
          override def publish[O](
              topicName: String,
              `object`: O
          ): CompletableFuture[Void] =
            ???
        }

        val ctx = new WindowContext(javaCtx)

        expect.all(
          ctx.inputTopics.map(_.value).contains(it1),
          ctx.inputTopics.map(_.value).contains(it2),
          ctx.outputTopic.value === ot,
          ctx.outputSchemaType.value === ost
        )
    }
  }

  test("WindowContext mapping of counter methods is correct") {
    forall {
      (
          key: String,
          amount: Long
      ) =>
        val counters = mutable.Map[String, Long]()

        val javaCtx = new JavaWindowContext {
          override def getTenant: String                       = ???
          override def getNamespace: String                    = ???
          override def getFunctionName: String                 = ???
          override def getFunctionId: String                   = ???
          override def getInstanceId: Int                      = ???
          override def getNumInstances: Int                    = ???
          override def getFunctionVersion: String              = ???
          override def getInputTopics: util.Collection[String] = ???
          override def getOutputTopic: String                  = ???
          override def getOutputSchemaType: String             = ???
          override def getLogger: Logger                       = ???
          override def incrCounter(key: String, amount: Long): Unit = {
            counters.put(key, getCounter(key) + amount)
            ()
          }

          override def getCounter(key: String): Long =
            counters.getOrElse(key, 0)
          override def putState(key: String, value: ByteBuffer): Unit    = ???
          override def getState(key: String): ByteBuffer                 = ???
          override def getUserConfigMap: util.Map[String, AnyRef]        = ???
          override def getUserConfigValue(key: String): Optional[AnyRef] = ???
          override def getUserConfigValueOrDefault(
              key: String,
              defaultValue: Any
          ): AnyRef                                                          = ???
          override def recordMetric(metricName: String, value: Double): Unit = ???
          override def publish[O](
              topicName: String,
              `object`: O,
              schemaOrSerdeClassName: String
          ): CompletableFuture[Void] = ???
          override def publish[O](
              topicName: String,
              `object`: O
          ): CompletableFuture[Void] =
            ???
        }

        val ctx  = new WindowContext(javaCtx)
        val res1 = ctx.getCounter(key)

        ctx.incrCounter(key, amount)
        val res2 = ctx.getCounter(key)

        ctx.incrCounter(key, amount)
        val res3 = ctx.getCounter(key)

        expect.all(
          res1 === 0,
          res2 === amount,
          res3 === amount * 2
        )
    }
  }

  test("WindowContext mapping of state is correct") {
    forall {
      (
          key: String,
          value: String
      ) =>
        val state = mutable.Map[String, ByteBuffer]()

        val javaCtx = new JavaWindowContext {
          override def getTenant: String                            = ???
          override def getNamespace: String                         = ???
          override def getFunctionName: String                      = ???
          override def getFunctionId: String                        = ???
          override def getInstanceId: Int                           = ???
          override def getNumInstances: Int                         = ???
          override def getFunctionVersion: String                   = ???
          override def getInputTopics: util.Collection[String]      = ???
          override def getOutputTopic: String                       = ???
          override def getOutputSchemaType: String                  = ???
          override def getLogger: Logger                            = ???
          override def incrCounter(key: String, amount: Long): Unit = ???
          override def getCounter(key: String): Long                = ???
          override def putState(key: String, value: ByteBuffer): Unit = {
            state.put(key, value)
            ()
          }
          override def getState(key: String): ByteBuffer                 = state.getOrElse(key, null)
          override def getUserConfigMap: util.Map[String, AnyRef]        = ???
          override def getUserConfigValue(key: String): Optional[AnyRef] = ???
          override def getUserConfigValueOrDefault(
              key: String,
              defaultValue: Any
          ): AnyRef                                                          = ???
          override def recordMetric(metricName: String, value: Double): Unit = ???
          override def publish[O](
              topicName: String,
              `object`: O,
              schemaOrSerdeClassName: String
          ): CompletableFuture[Void] = ???
          override def publish[O](
              topicName: String,
              `object`: O
          ): CompletableFuture[Void] =
            ???
        }

        val ctx  = new WindowContext(javaCtx)
        val res1 = ctx.getState(key)

        val bytes = ByteBuffer.wrap(value.getBytes)
        ctx.putState(key, bytes)

        val res2 = ctx.getState(key)

        expect.all(
          res1.isEmpty,
          res2.contains(bytes)
        )
    }
  }

  test("WindowContext mapping of user config is correct") {
    forall {
      (
          key: String,
          value: Int,
          defaultValue: Int
      ) =>
        val map = new util.HashMap[String, Object]()

        val javaCtx = new JavaWindowContext {
          override def getTenant: String                              = ???
          override def getNamespace: String                           = ???
          override def getFunctionName: String                        = ???
          override def getFunctionId: String                          = ???
          override def getInstanceId: Int                             = ???
          override def getNumInstances: Int                           = ???
          override def getFunctionVersion: String                     = ???
          override def getInputTopics: util.Collection[String]        = ???
          override def getOutputTopic: String                         = ???
          override def getOutputSchemaType: String                    = ???
          override def getLogger: Logger                              = ???
          override def incrCounter(key: String, amount: Long): Unit   = ???
          override def getCounter(key: String): Long                  = ???
          override def putState(key: String, value: ByteBuffer): Unit = ???
          override def getState(key: String): ByteBuffer              = ???

          override def getUserConfigMap: util.Map[String, Object] = map
          override def getUserConfigValue(key: String): Optional[AnyRef] =
            Optional.ofNullable(map.get(key))
          override def getUserConfigValueOrDefault(
              key: String,
              defaultValue: AnyRef
          ): AnyRef =
            Optional.ofNullable(map.get(key)).orElse(defaultValue)
          override def recordMetric(metricName: String, value: Double): Unit = ???
          override def publish[O](
              topicName: String,
              `object`: O,
              schemaOrSerdeClassName: String
          ): CompletableFuture[Void] = ???
          override def publish[O](
              topicName: String,
              `object`: O
          ): CompletableFuture[Void] =
            ???
        }

        val ctx = new WindowContext(javaCtx)

        val res1 = ctx.userConfigValue(key)
        val res2 = ctx.userConfigValueOrElse(key, defaultValue)

        map.put(key, Integer.valueOf(value))

        expect.all(
          res1.isEmpty,
          res2 === defaultValue,
          ctx.userConfigMap.size === 1,
          ctx.userConfigMap.get(key).contains(value),
          ctx.userConfigValue[Int](key).contains(value)
        )
    }
  }

  test("WindowContext can publish messages") {
    forall {
      (
          ot: String,
          sc: String,
          value: Int
      ) =>
        var i = 0

        def publishMessage: CompletableFuture[Void] =
          Future({ i = i + 1 }).toJava.toCompletableFuture
            .thenApply(_ => null) // converting to void

        val javaCtx = new JavaWindowContext {
          override def getTenant: String                              = ???
          override def getNamespace: String                           = ???
          override def getFunctionName: String                        = ???
          override def getFunctionId: String                          = ???
          override def getInstanceId: Int                             = ???
          override def getNumInstances: Int                           = ???
          override def getFunctionVersion: String                     = ???
          override def getInputTopics: util.Collection[String]        = ???
          override def getOutputTopic: String                         = ???
          override def getOutputSchemaType: String                    = ???
          override def getLogger: Logger                              = ???
          override def incrCounter(key: String, amount: Long): Unit   = ???
          override def getCounter(key: String): Long                  = ???
          override def putState(key: String, value: ByteBuffer): Unit = ???
          override def getState(key: String): ByteBuffer              = ???

          override def getUserConfigMap: util.Map[String, Object]        = ???
          override def getUserConfigValue(key: String): Optional[AnyRef] = ???
          override def getUserConfigValueOrDefault(
              key: String,
              defaultValue: AnyRef
          ): AnyRef                                                          = ???
          override def recordMetric(metricName: String, value: Double): Unit = ???

          override def publish[O](
              topicName: String,
              `object`: O,
              schemaOrSerdeClassName: String
          ): CompletableFuture[Void] = publishMessage

          override def publish[O](
              topicName: String,
              `object`: O
          ): CompletableFuture[Void] = publishMessage
        }

        val ctx = new WindowContext(javaCtx)
        Await.result(ctx.publish[Int](OutputTopic(ot), value), 5.seconds)
        val res1 = i

        Await.result(ctx.publish[Int](OutputTopic(ot), value, sc), 5.seconds)
        val res2 = i

        expect.all(
          res1 === 1,
          res2 === 2
        )
    }
  }
}
