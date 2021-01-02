package macros.internals

import java.util.function.Predicate
import java.util.regex.Pattern
import scala.reflect.macros.{ ParseException, TypecheckException }
import scala.util.{ Failure, Success }

protected[macros] trait IllTyped extends Util {
  import c.universe._

  val code: String
  val expectedErrorPattern: String

  val tname1: TermName = TermName(c.freshName())
  val tname2: TermName = TermName(c.freshName())

  val patternPredicate: String => Predicate[String] =
    Pattern.compile(_, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).asMatchPredicate()

  val (errorPredicate: Predicate[String], errorMessage: String) = expectedErrorPattern match {
    case null            => (patternPredicate(".*"), "Expected some type-checking error.")
    case s if s.isBlank  => (patternPredicate(".*"), "Expected some type-checking error.")
    case s if !s.isBlank => (patternPredicate(s), s"Expected error matching: ${showRaw(s)}")
  }

  val isExpectedError: Throwable => Boolean = (e: Throwable) => errorPredicate.test(e.getMessage)

  def check(code: String): scala.util.Try[Tree] = scala.util.Try {
    val envelopedCode = s"""object $tname1 { val $tname2 = { $code } }"""
    c.typecheck(c.parse(envelopedCode))
  }

  def main: Option[(Position, String)] = check(code) match {
    case Failure(e: TypecheckException) =>
      val msg = s"""Type-checking failed, but in an unexpected way.
                   |${e.getMessage}
                   |$errorMessage""".stripMargin
      if (!isExpectedError(e)) Some(c.enclosingPosition, msg) else None
    case Failure(e: ParseException) =>
      val msg = s"""Unable to type-check due to parsing error.
                   |${e.getMessage}
                   |$errorMessage""".stripMargin
      Some(c.enclosingPosition, msg)
    case Failure(_) => Some(c.enclosingPosition, s"Type-checking failed unexpectedly. $errorMessage")
    case Success(_) => Some(c.enclosingPosition, s"Type-checking passed unexpectedly. $errorMessage")
  }

}
