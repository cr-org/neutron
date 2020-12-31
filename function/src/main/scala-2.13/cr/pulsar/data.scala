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

//import io.estatico.newtype.macros._

object data {
  case class Tenant(value: String)
  case class Namespace(value: String)
  case class FunctionName(value: String)
  case class FunctionId(value: String)
  case class InstanceId(value: Int)
  case class NumInstances(value: Int)
  case class FunctionVersion(value: String)
  case class InputTopic(value: String)
  case class OutputTopic(value: String)
  case class OutputSchemaType(value: String)

  // getting these errors with newtype on scala 2.13.x, not sure why, it works in the core module
  // [error] /home/gvolpe/workspace/cr/neutron/function/src/main/scala-2.13/cr/pulsar/data.scala:22:23: macro annotation could not be expanded (you cannot use a macro annotation in the same compilation run that defines it)
  // [error]   @newtype case class Tenant(value: String)
  //
  //@newtype case class Tenant(value: String)
  //@newtype case class Namespace(value: String)
  //@newtype case class FunctionName(value: String)
  //@newtype case class FunctionId(value: String)
  //@newtype case class InstanceId(value: Int)
  //@newtype case class NumInstances(value: Int)
  //@newtype case class FunctionVersion(value: String)
  //@newtype case class InputTopic(value: String)
  //@newtype case class OutputTopic(value: String)
  //@newtype case class OutputSchemaType(value: String)
}
