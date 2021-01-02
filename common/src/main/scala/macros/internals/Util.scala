package macros.internals

import scala.collection.immutable.ListMap
import scala.reflect.macros.blackbox

/** Common utility class for all def and annotation macro implementations in this project. */
protected[internals] trait Util {
  val c: blackbox.Context
  import c.universe._

  val isMacroLogEnabled: Boolean = sys.env.get("DEBUG_MACROS") match {
    case Some(value) => value.toBooleanOption.getOrElse(false)
    case None        => false
  }

  val (macroName: Name, macroTree: Tree, macroType: MacroType.Value) = c.macroApplication match {
    // annotation macro class definition with the special `macroTransform` method
    case Apply(Select(Apply(Select(New(Ident(name)), termNames.CONSTRUCTOR), _), _), _) =>
      (name, c.typecheck(c.prefix.tree), MacroType.Annotation)
    // apply def macro method definition, object definition mimicking a method
    case Apply(Select(t @ _, name), _) if name == TermName("apply") =>
      (toName(t.symbol), c.macroApplication, MacroType.Def)
    // other def macro method definitions
    case Apply(t @ Select(_, name), _) if name != TermName("apply") =>
      (toName(t.symbol), c.macroApplication, MacroType.Def)
  }

  /** Message to show on macro init. */
  val macroInitMessage: String = s"Init `$macroName` $macroType macro expansion."

  /** Message to show on macro exit. */
  val macroExitMessage: String = s"Exit `$macroName` $macroType macro expansion."

  /** Parameters names and their values for the macro application. */
  val macroParameters: ListMap[String, Nothing] = extractParameters(macroTree) match {
    case Left((_, _)) => ListMap.empty[String, Nothing]
    case Right(value) => value
  }

  /** Takes a symbol and returns its simplified name.
   *
   * @param s symbol for named class or object
   * @return  de-sugared name for the class or object
   */
  def toName(s: Symbol): Name = TermName(s.name.toString.replaceAll(".<init>$", ""))

  /** Extracts any combination of positional, named and default arguments for parameters.
   *
   * [[https://docs.scala-lang.org/sips/named-and-default-arguments.html#implementation]]
   *
   * @param  t abstract syntax tree for a constructor or method call
   * @tparam A type of the parameters
   * @return   ordered map of parameter names and their values
   *
   * @note     Does not support call to definitions with multiple parameter lists.
   */
  def extractParameters[A <: Any](t: Tree): Either[(Position, String), ListMap[String, A]] = {

    val toNameList: Select => List[String] = _.symbol.typeSignature.paramLists.flatten.map(_.name.toString)

    val toNameValueMap: List[Tree] => ListMap[String, A] = _.map({
      case v: ValDef => ListMap(v.name.toString -> evaluate(v.rhs).get)
    }).reduceOption(_ ++ _)
      .getOrElse(ListMap.empty)

    def parametersFromApplyTree(s: Select, li: List[Tree]): ListMap[String, A] = {
      val parameters = toNameList(s)

      li.zipWithIndex
        .map({
          case (t: Tree, i: Int) => ListMap(parameters(i) -> evaluate(t).get)
        })
        .reduceOption(_ ++ _)
        .getOrElse(ListMap.empty)
    }

    def parametersFromBlockTree(s: Select, l1: List[Tree], l2: List[Tree]): ListMap[String, A] = {
      val parameters   = toNameList(s)
      val valDef       = toNameValueMap(l1)
      val valDefValues = valDef.values.toList

      l2.zipWithIndex
        .map({
          case (Ident(TermName(s)), i: Int) => ListMap(parameters(i) -> valDef(s))
          case (_, i: Int)                  => ListMap(parameters(i) -> valDefValues(i))
        })
        .reduceOption(_ ++ _)
        .getOrElse(ListMap.empty)
    }

    scala.util.Try {
      t match {
        case Apply(s @ Select(_, _), li)                          => parametersFromApplyTree(s, li)
        case Apply(TypeApply(s @ Select(_, _), _), li)            => parametersFromApplyTree(s, li)
        case Block(l1, Apply(s @ Select(_, _), l2))               => parametersFromBlockTree(s, l1, l2)
        case Block(l1, Apply(TypeApply(s @ Select(_, _), _), l2)) => parametersFromBlockTree(s, l1, l2)
        case _                                                    => ???
      }
    }.fold(fa => Left(t.pos, fa.getMessage), Right(_))
  }

  /** Takes an abstract syntax tree and evaluates it to a value of type `A`.
   *
   * @param  t abstract syntax tree for value
   * @tparam A type of the value
   * @return   value of type `A`
   *
   * @note     Uses a workaround for possible bug [[https://users.scala-lang.org/t/3181]].
   *           As a result, [[scala.Option]] type may silently evaluate to [[scala.None]].
   */
  def evaluate[A](t: Tree): util.Try[A] = util.Try {
    val optionType   = c.mirror.staticClass("scala.Option").toTypeConstructor
    val isOptionType = t.tpe.finalResultType.typeConstructor =:= optionType
    val isUnchecked = t.symbol match {
      case null => false
      case _    => t.symbol.typeSignature.toString.endsWith("@scala.annotation.unchecked.uncheckedVariance")
    }

    // A hack to work around https://users.scala-lang.org/t/3181
    if (isOptionType && isUnchecked)
      c.eval(c.Expr[A](q"None"))
    else
      c.eval(c.Expr[A](c.untypecheck(t.duplicate)))
  }

  type MacroType = MacroType.Value
  object MacroType extends Enumeration {
    val Def: MacroType        = Value("def")
    val Annotation: MacroType = Value("annotation")
  }

  object MacroError extends Error(s"Macro $macroName expansion failed.") {
    def apply: Error                        = this
    def apply(message: String): String      = getMessage + sys.props("line.separator") + message
    def apply(throwable: Throwable): String = getMessage + sys.props("line.separator") + throwable.getMessage
  }

}
