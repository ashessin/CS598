package com.ashessin.cs598.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Annotation macro to map Caliban Selection Builder value definitions to custom case classes.
 */
@compileTimeOnly("Enable macro paradise to expand macro annotations.")
class MapSelection() extends StaticAnnotation {
  //noinspection ScalaUnusedSymbol
  def macroTransform(annottees: Any*): Any = macro MapSelectionMacro.impl
}

private object MapSelectionMacro {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[c.universe.Block] = {
    import c.universe._

    val caseClassNamePrefix: String = "Gh"
    val anyType: Type               = typeOf[Any]
    val selectionBuilderType: Type  = c.mirror.staticClass("caliban.client.SelectionBuilder").toTypeConstructor

    /**
     * Validates the type ascribed to annotated value definition.
     *
     * @param tptTree AST of the type ascribed
     */
    def validateTpt(tptTree: c.Tree): Option[String] = {

      val tptType: c.universe.Type = tptTree.tpe.typeConstructor
      val tptArgTypes: Seq[Type]   = tptTree.tpe.typeArgs
      val validTptTypes: Seq[Type] = List(anyType, selectionBuilderType)

      // Value type should be Any or caliban.client.SelectionBuilder
      if (!validTptTypes.exists(_ =:= tptType))
        return Some(s"Value type should be Any or caliban.client.SelectionBuilder; got $tptType")

      // Value type's second type argument should be Any
      if ((tptType =:= selectionBuilderType) && (tptArgTypes.size == 2) && !(tptArgTypes(1) =:= anyType))
        return Some(s"Value type's second type argument should be Any; got ${tptArgTypes(1)}")

      None
    }

    /**
     * Validates the body of annotated value definition.
     *
     * @param exprTree AST of the body
     */
    def validateRhs(exprTree: c.Tree): Option[String] = {

      val exprType: c.universe.Type        = exprTree.tpe.typeConstructor
      val exprClassName: String            = exprTree.getClass.getSimpleName
      val validExprType: c.universe.Type   = selectionBuilderType
      val validExprClassNames: Seq[String] = List("Apply", "Select")

      // Body should be a simple value application or a member selection <qualifier> . <name>
      // https://www.scala-lang.org/api/2.12.12/scala-reflect/scala/reflect/api/Trees$Apply.html
      // https://www.scala-lang.org/api/2.12.12/scala-reflect/scala/reflect/api/Trees$Select.html
      if (!validExprClassNames.contains(exprClassName))
        return Some(s"Expected RHS tree of ${validExprClassNames.mkString(", ")}; got $exprClassName")

      // Body should be of type caliban.client.SelectionBuilder
      if (!(exprType =:= validExprType))
        return Some(s"Expected RHS type $validExprType; got $exprType")

      None
    }

    /**
     * Filters a selection tree for unit selections.
     *
     * @param exprTree a selection tree of one or more selections combined by ~
     * @return a list of unit selections that were combined by ~
     */
    def extractUnitSelections(exprTree: c.Tree): Seq[Tree] =
      exprTree.filter(tree => tree.children.size == 1 && tree.symbol.isMethod && tree.symbol.name != TermName("$tilde"))

    val result: c.universe.Tree = annottees.map(_.tree).toList match {
      case q"$mods val $tname: $tpt = $expr" :: Nil =>
        val tptTree: c.Tree  = c.typecheck(tq"$tpt", c.TYPEmode)
        val exprTree: c.Tree = c.typecheck(tq"$expr", c.TYPEmode)

        // Additional validations
        validateTpt(tptTree) match {
          case Some(message: String) => c.abort(tptTree.pos, message)
          case None                  =>
        }
        validateRhs(exprTree) match {
          case Some(message: String) => c.abort(exprTree.pos, message)
          case None                  =>
        }

        val unitSelections: Seq[c.universe.Tree] = extractUnitSelections(exprTree)
        // The case class name
        val caseClassName: c.universe.TypeName = TypeName(
          caseClassNamePrefix + tptTree.tpe.typeArgs.head.typeSymbol.name
        )
        // A list of parameters for the case class
        val parameters: Seq[Tree] = unitSelections.map { unitSelection =>
          q"val ${unitSelection.symbol.name.toTermName}: ${unitSelection.tpe.typeArgs.last}"
        }

        c.info(c.enclosingPosition, s"Creating new case class `$caseClassName`.", force = false)

        // Map via. map or mapN
        val mapfunctionName: c.universe.TermName = exprTree.getClass.getSimpleName match {
          case "Apply"  => TermName("mapN")
          case "Select" => TermName("map")
        }

        q"""
           case class $caseClassName(..$parameters)
           $mods val $tname: $tpt = $expr.$mapfunctionName(${caseClassName.toTermName})
        """
      case _ => c.abort(c.enclosingPosition, "Annotation @MapSelection can be used only with value definition.")
    }
    c.Expr[Block](result)
  }
}
