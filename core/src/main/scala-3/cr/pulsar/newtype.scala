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

import scala.compiletime.erasedValue
import scala.concurrent.duration.FiniteDuration

object NewType:
  class GenNewType[A]:
    opaque type Type = A
    def apply(a: A): Type = a
    extension (a: Type) def value: A = a
  end GenNewType

  class NewTypeBool extends GenNewType[Boolean]:
    override opaque type Type = Boolean
    override def apply(a: Boolean): Type = a
    extension (a: Type) override def value: Boolean = a
  end NewTypeBool

  class NewTypeDouble extends GenNewType[Double]:
    override opaque type Type = Double
    override def apply(a: Double): Type = a
    extension (a: Type) override def value: Double = a
  end NewTypeDouble

  class NewTypeInt extends GenNewType[Int]:
    override opaque type Type = Int
    override def apply(a: Int): Type = a
    extension (a: Type) override def value: Int = a
  end NewTypeInt

  class NewTypeLong extends GenNewType[Long]:
    override opaque type Type = Long
    override def apply(a: Long): Type = a
    extension (a: Type) override def value: Long = a
  end NewTypeLong

  class NewTypeString extends GenNewType[String]:
    override opaque type Type = String
    override def apply(a: String): Type = a
    extension (a: Type) override def value: String = a
  end NewTypeString

  class NewTypeFiniteDuration extends GenNewType[FiniteDuration]:
    override opaque type Type = FiniteDuration
    override def apply(a: FiniteDuration): Type = a
    extension (a: Type) override def value: FiniteDuration = a
  end NewTypeFiniteDuration

  transparent inline def of[A] = inline erasedValue[A] match {
    case _: Boolean        => new NewTypeBool
    case _: Double         => new NewTypeDouble
    case _: FiniteDuration => new NewTypeFiniteDuration
    case _: Int            => new NewTypeInt
    case _: Long           => new NewTypeLong
    case _: String         => new NewTypeString
    case _                 => new GenNewType[A]
  }
end NewType
