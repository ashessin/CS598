package macros

import scala.annotation.{ compileTimeOnly, StaticAnnotation }
import scala.language.experimental.macros

/** Annotation macro to map [[caliban.client.SelectionBuilder]] value definitions to case classes.
 *
 * @param classNamePrefix prefix for generated case class names
 */
@compileTimeOnly("Enable macro paradise to expand macro annotations.")
class mapSelectionBuilder(classNamePrefix: String = "") extends StaticAnnotation {
  //noinspection ScalaUnusedSymbol
  def macroTransform(annottees: Any*): Any = macro MapSelectionBuilderMacro.impl
}

import scala.reflect.macros.whitebox

class MapSelectionBuilderMacro(val c: whitebox.Context) extends internals.MapSelectionBuilder {

  import c.universe._

  lazy val classNamePrefix: String = macroParameters.getOrElse("classNamePrefix", "")

  def impl(annottees: c.Expr[Any]*): c.Expr[Block] = {
    if (isMacroLogEnabled) c.info(c.enclosingPosition, macroInitMessage, force = false)

    val block: Tree = SelectionBuilderValDef(annottees.map(_.tree).toList) match {
      case Right(selectionValDef) => selectionValDef.transform
      case Left((pos, msg)) =>
        c.error(pos.asInstanceOf[Position], msg)
        c.abort(c.enclosingPosition, null)
    }

    println(showCode(block))
    if (isMacroLogEnabled) c.info(c.enclosingPosition, macroExitMessage, force = false)

    c.Expr[Block](block)
  }

}
