/*
 * Copyright (c) 2013-16 Miles Sabin
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

package macros

import scala.language.experimental.macros

/** Def macro to checks if a code fragment typecheck.
 *
 * @note credited to Stefan Zeiger (@StefanZeiger)
 */
object illTyped {
  def apply(code: String): Unit = macro IllTypedMacro.implNoExpectedErrorPattern
  def apply(code: String, expectedErrorPattern: String): Unit = macro IllTypedMacro.impl
}

import scala.reflect.macros.blackbox

class IllTypedMacro(val c: blackbox.Context) extends internals.IllTyped {

  import c.universe._

  lazy val code: String                 = macroParameters("code")
  lazy val expectedErrorPattern: String = macroParameters.getOrElse("expectedErrorPattern", None.orNull)

  def implNoExpectedErrorPattern(code: Tree): Tree = impl(code, None.orNull)

  //noinspection ScalaUnusedSymbol
  def impl(code: Tree, expectedErrorPattern: Tree): Tree = {
    if (isMacroLogEnabled) c.info(c.enclosingPosition, macroInitMessage, force = false)

    main match {
      case Some((pos, msg)) => c.error(pos, msg)
      case None             =>
    }

    if (isMacroLogEnabled) c.info(c.enclosingPosition, macroExitMessage, force = false)

    q"()"
  }

}
