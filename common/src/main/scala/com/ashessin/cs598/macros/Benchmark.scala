package com.ashessin.cs598.macros

import scala.annotation.{ compileTimeOnly, StaticAnnotation }
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Annotation macro to benchmark ie. show time consumed by a method.
 */
@compileTimeOnly("Enable macro paradise to expand macro annotations.")
class Benchmark extends StaticAnnotation {
  //noinspection ScalaUnusedSymbol
  def macroTransform(annottees: Any*): Any = macro BenchmarkMacro.impl
}

object BenchmarkMacro {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[c.universe.DefDef] = {
    import c.universe._

    val result: c.universe.Tree = annottees.map(_.tree).toList match {
      case q"$mods def $tname[..$tparams](...$paramss): $tpt = $expr" :: Nil =>
        q"""$mods def $tname[..$tparams](...$paramss): $tpt = {
            val start = System.nanoTime()
            val result = $expr
            val end = System.nanoTime()
            println("Method `" +  ${tname.toString} + "` took: " + (end - start)/1000000 + "ms\n")
            result
          }"""
      case _ => c.abort(c.enclosingPosition, "Annotation @Benchmark can be used only with method definition.")
    }
    c.Expr[DefDef](result)
  }
}
