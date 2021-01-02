package macros.internals

import scala.collection.immutable.Queue
import scala.reflect.macros.TypecheckException
import scala.util.{ Failure, Success }

protected[macros] trait MapSelectionBuilder extends Util {
  import c.universe._

  import scala.reflect.api.Position

  val classNamePrefix: String

  val unexpectedMacroUsage: String =
    MacroError(s"Macro $macroName is used in an unexpected way.")
  val invalidMacroUsage: String =
    MacroError(s"Macro $macroName can be used only on a mutable or immutable value definition.")
  val invalidTpt: String =
    MacroError("Value definition's type should be of form caliban.client.SelectionBuilder[<origin>, _].")
  val invalidRhs: String =
    MacroError("Value definition's right hand side should be a value application or a member selection.")

  //noinspection ScalaUnusedSymbol
  val toTree: Seq[Tree] => Either[(Position, String), Tree] = {
    case (t @ q"$mods val $name: $tpt = $rhs") :: Nil => Right(t)
    case (t @ q"$mods var $name: $tpt = $rhs") :: Nil => Right(t)
    case _                                            => Left(c.enclosingPosition, invalidMacroUsage)
  }

  //noinspection ScalaUnusedSymbol
  val toTypedValDef: Tree => Either[(Position, String), ValDef] = (tree: Tree) =>
    scala.util.Try(c.typecheck(tree)) match {
      case Success(v: ValDef)                    => Right(v)
      case Failure(TypecheckException(pos, msg)) => Left(pos, MacroError(msg))
      case _                                     => Left(c.enclosingPosition, invalidMacroUsage)
    }

  //noinspection ScalaUnusedSymbol
  val toSelectionValDef: ValDef => Either[(Position, String), SelectionBuilderValDef] = (valDef: ValDef) =>
    (valDef.tpt, valDef.rhs) match {
      // TPT is an Existential Type Tree of correct form and RHS is a Select Tree
      case (tq"caliban.client.SelectionBuilder[$origin, _]", q"$expr.$tname") =>
        Right(SelectionBuilderValDef(valDef.mods, valDef.name, valDef.tpt, valDef.rhs))
      // TPT is an Existential Type Tree of correct form and RHS is an Apply Tree
      case (tq"caliban.client.SelectionBuilder[$origin, _]", q"$expr(...$exprss)") =>
        Right(SelectionBuilderValDef(valDef.mods, valDef.name, valDef.tpt, valDef.rhs))
      // invalid trees, try to provide as much information as possible
      case (t, q"$expr.$tname")                                 => Left(t.pos, invalidTpt)
      case (t, q"$expr(...$exprss)")                            => Left(t.pos, invalidTpt)
      case (tq"caliban.client.SelectionBuilder[$origin, _]", t) => Left(t.pos, invalidRhs)
      case _                                                    => Left(valDef.pos, unexpectedMacroUsage)
    }

  import scalax.collection.GraphEdge.{ DiEdge, EdgeCopy, ExtendedKey, NodeProduct }
  import scalax.collection.GraphPredef.{ OuterEdge, _ }
  import scalax.collection.edge.WDiEdge
  import scalax.collection.immutable.Graph

  type SelectionBuilderGraph = Graph[SelectionBuilderNode, SelectionBuilderEdge]

  /**
   *
   */
  trait SelectionBuilderNode extends Product with Serializable {
    override def toString: String = if (productArity > 0) productElement(0).toString else super.toString
  }

  /**  A custom node type that represents a class name.
   *
   * @param name  the name of this node
   */
  case class ObjectNode(name: TypeName) extends SelectionBuilderNode

  /** A custom node type that represents a parameter type.
   *
   * @param name the name of this node
   */
  case class ScalarNode(name: Type) extends SelectionBuilderNode

  /** A custom edge type that connects two nodes and represents parameter name.
   *
   * @param fromNode  source node
   * @param toNode    destination node
   * @param name      label for this edge
   * @param position  a weighted number relative to other edges
   * @tparam N        the type of nodes connected by this edge
   */
  protected case class SelectionBuilderEdge[+N](fromNode: N, toNode: N, name: TermName, position: Double)
      extends WDiEdge[N](NodeProduct(fromNode, toNode), position)
      with ExtendedKey[N]
      with EdgeCopy[SelectionBuilderEdge]
      with OuterEdge[N, SelectionBuilderEdge] {
    def keyAttributes = Seq(name)

    override def copy[NN](newNodes: Product) = new SelectionBuilderEdge[NN](newNodes, name, position)

    //noinspection ScalaUnusedSymbol
    private def this(nodes: Product, name: TermName, position: Double) = this(
      nodes.productElement(0).asInstanceOf[N],
      nodes.productElement(1).asInstanceOf[N],
      name,
      position
    )

    override protected def attributesToString = s"%$name"
  }

  protected case class SelectionBuilderValDef(mods: Modifiers, name: TermName, lhs: Tree, rhs: Tree) {

    import SelectionBuilderEdge._

    /** Transforms the rhs to a generate case classes.
     *
     * @return an empty abstract syntax tree
     */
    def transform: Tree = {
      val reservedWords = List(
        // format: off
        "abstract"   , "case"       , "catch"      , "class"      , "def",
        "do"         , "else"       , "extends"    , "false"      , "final",
        "finally"    , "for"        , "forSome"    , "if"         , "implicit",
        "import"     , "lazy"       , "macro"      , "match"      , "new",
        "null"       , "object"     , "override"   , "package"    , "private",
        "protected"  , "return"     , "sealed"     , "super"      , "this",
        "throw"      , "trait"      , "try"        , "true"       , "type",
        "val"        , "var"        , "while"      , "with"       , "yield"
      // format: on
      )
      val typeRange     = 'A' to 'Z'
      val (graph: SelectionBuilderGraph, objectNodes: Queue[ObjectNode]) = toGraph(rhs) match {
        case (g, q, _) => (g, q)
      }
      val edgeOrdering = graph.EdgeOrdering(graph.Edge.WeightOrdering.compare)
      // just show generated case classes

      val caseClasses = objectNodes.map { objectNode =>
        val orderedSuccessors = (graph get objectNode).outerEdgeTraverser
          .withOrdering(edgeOrdering)
          .map(e => {
            if (reservedWords.contains(e.name.toString))
              s"`${e.name}`: ${e._2}"
            else
              s"${e.name}: ${e._2}"
          })
          .zipWithIndex
          .map {
            case (str, i) if str.contains("[A]") =>
              val typeParam = typeRange(i).toString
              (Some(typeParam), str.replace("[A]", s"[$typeParam]"))
            case (str, i) if str.contains(": A") =>
              val typeParam = typeRange(i).toString
              (Some(typeParam), str.replace(": A", s": $typeParam"))
            case (str, _) => (None, str)
          }
        val parameters = orderedSuccessors
          .map({ case (_, str) => str})
        val typeParams = orderedSuccessors
          .filter({ case (opt, _) => opt.nonEmpty })
          .map(_._1.get)

        if (typeParams.isEmpty) {
          c.parse(s"case class $objectNode(${parameters.mkString(", ")})")
        } else
          c.parse(s"case class $objectNode[${typeParams.mkString(", ")}](${parameters.mkString(", ")})")
      }

      q"{ ..$caseClasses; $mods val $name: $lhs = $rhs }"
    }

    /** Recursively transforms an abstract syntax tree to a graph.
     *
     * @param t abstract syntax tree that needs to be transformed
     * @param g an incomplete graph of transformed abstract syntax trees
     * @param q object nodes in incomplete graph of transformed abstract syntax trees
     * @param w edge weight to use when adding a connection
     * @return  a graph with nodes and connecting edges
     */
    def toGraph(
      t: Tree,
      g: Graph[SelectionBuilderNode, SelectionBuilderEdge] = Graph.empty,
      q: Queue[ObjectNode] = Queue.empty,
      w: Double = 0
    ): (Graph[SelectionBuilderNode, SelectionBuilderEdge], Queue[ObjectNode], Double) = {
      var graph: Graph[SelectionBuilderNode, SelectionBuilderEdge] = g
      var queue: Queue[ObjectNode]                                 = q
      var weight: Double                                           = w

      t match {

        case select @ Select(qualifier, name) if name == TermName("$tilde") =>
          toGraph(qualifier, graph, queue, weight)

        case select @ Select(qualifier, name) if qualifier.symbol.isModule =>
          val className: TypeName          = TypeName(classNamePrefix + qualifier.symbol.name)
          val fromNode: ObjectNode         = ObjectNode(className)
          val classParameterName: TermName = name.toTermName
          val classParameterType: Type     = select.tpe.finalResultType.typeArgs.last
          val toNode: ScalarNode           = ScalarNode(classParameterType)

          weight = weight + 1
          graph = graph.union(Graph(fromNode ~> toNode % (classParameterName, weight)))

          val preUnionSize  = g.nodes.count(_.diSuccessors.nonEmpty)
          val postUnionSize = graph.nodes.count(_.diSuccessors.nonEmpty)

          if (postUnionSize > preUnionSize)
            queue = q :+ fromNode

          (graph, queue, weight)

        case apply @ Apply(Apply(TypeApply(fun, _), _), args @ List(_*)) =>
          toGraph(fun, graph, queue, weight) match {
            case (g, q, w) => graph = g; queue = q; weight = w
          }
          for (arg <- args) {
            toGraph(arg, graph, queue, weight) match {
              case (g, q, w) => graph = g; queue = q; weight = w
            }
          }
          (graph, queue, weight)

        case apply @ Apply(TypeApply(fun, _), args @ List(_*)) =>
          toGraph(fun, graph, queue, weight) match {
            case (g, q, w) => graph = g; queue = q; weight = w
          }
          for (arg <- args) {
            toGraph(arg, graph, queue, weight) match {
              case (g, q, w) => graph = g; queue = q; weight = w
            }
          }
          (graph, queue, weight)

        case apply @ Apply(select @ Select(_, _), _) =>
          toGraph(select, graph, queue, weight)
      }
    }

  }

  object SelectionBuilderEdge {
    implicit final class ImplicitEdge[A <: SelectionBuilderNode](val e: DiEdge[A]) {
      def %(name: TermName, position: Double) = new SelectionBuilderEdge[A](e.source, e.target, name, position)
    }
  }

  object SelectionBuilderValDef {
    def apply(trees: Seq[Tree]): Either[(Position, String), SelectionBuilderValDef] =
      toTree(trees).flatMap(toTypedValDef(_)).flatMap(toSelectionValDef(_))
  }

}
