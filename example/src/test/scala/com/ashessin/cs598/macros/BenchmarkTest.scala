package com.ashessin.cs598.macros

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BenchmarkTest extends AnyFlatSpec with Matchers {

  "Benchmark Macro Annotation" should "compile when applied to a method definition" in {
    // $mods def $name[..$tparams](...$paramss): $tpt = $body
    """
    @Benchmark
    def printHelloWorld(): Unit = {
      println("Hello World!")
    }
    """ should compile
  }

  it should "not compile when applied to a val definition" in {
    // $mods val $name: $tpt = $rhs
    """
    @Benchmark private val printHelloWorld: Unit = { println("Hello World!") }
    """ shouldNot compile
  }

  it should "not compile when applied to a var definition" in {
    // $mods var $name: $tpt = $rhs
    """
    @Benchmark private val printHelloWorld: Unit = { println("Hello World!") }
    """ shouldNot compile
  }

  it should "not compile when applied to a type definition" in {
    // $mods type $name[..$tparams] >: $low <: $high
    """
    @Benchmark type Foo[T] <: List[T]
    """ shouldNot compile
    // $mods type $name[..$args] = $tpt
    """
    @Benchmark type Foo[T] = List[T]
    """ shouldNot compile
  }

  "Benchmark Macro Annotated method" should "additionally print to console the time taken" in {
    val message = "Hello World!"
    class Foo extends OutputCollector {
      @Benchmark
      def printHelloWorld(): Unit = println(message)
    }

    val foo = new Foo
    // temporarily suppress output
    Console.withOut(new java.io.ByteArrayOutputStream()) {
      foo.printHelloWorld()
    }

    foo.messages should have size 2
    foo.messages should contain(message)
    foo.messages.last should startWith("Method `printHelloWorld` took: ")
  }

}
