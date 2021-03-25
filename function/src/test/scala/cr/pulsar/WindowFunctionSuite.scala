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

import cr.pulsar.FunctionInput._

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object WindowFunctionSuite extends SimpleIOSuite with Checkers {
  test("WindowFunction can convert numbers to strings") {
    forall { (n1: Int, n2: Int) =>
      val f = new WindowFunction[Int, String] {
        override def handle(input: Seq[Record[Int]], ctx: WindowContext): String =
          input.map(_.value).sum.toString
      }

      val result = f.process(input(Seq(n1, n2)), emptyWindowCtx)
      expect.same(result, (n1 + n2).toString)
    }
  }

  test("WindowFunction can do side effects") {
    forall { (n1: Int, n2: Int) =>
      var i = 0
      val f = new WindowFunction[Int, Unit] {
        override def handle(input: Seq[Record[Int]], ctx: WindowContext): Unit =
          i = input.size
      }

      f.process(input(Seq(n1, n2)), emptyWindowCtx)
      expect.same(i, 2)
    }
  }
}
