package com.ashessin.cs598.macros

/**
 * A helper to capture what's printed on standard output when using `println` or `print`.
 */
trait OutputCollector {

  private var _messages: Seq[String] = Seq()

  def messages: Seq[String] = _messages

  /**
   * Captures and Prints an object to `out` using its `toString` method.
   *
   * @param x   the object to print; may be null
   * @param raw makes the string capture raw
   */
  protected def print(x: Any, raw: Boolean = false): Unit = {
    _messages = _messages :+ (if (raw) escape(x.toString) else x.toString)
    Console.println(x)
  }

  /**
   * Captures and Prints an object to `out` using its `toString` method, followed by a newline character.
   *
   * @param x   the object to print
   * @param raw makes the string capture raw
   */
  protected def println(x: Any = sys.props("line.separator"), raw: Boolean = false): Unit = {
    _messages = _messages :+ (if (raw) escape(x.toString) else x.toString)
    Console.println(x)
  }

  /**
   * Returns the raw string using runtime reflection.
   *
   * @param x some string
   * @return a raw representation of the string
   */
  private def escape(x: String): String = {
    import scala.reflect.runtime.universe._
    Literal(Constant(x)).toString
  }
}
