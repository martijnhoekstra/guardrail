package dev.guardrail.generators.syntax

import dev.guardrail.generators.java.syntax._
import scala.util.Random
import scala.util.{ Failure, Try }
import com.github.javaparser.StaticJavaParser
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

object JavaSyntaxTest {
  val TEST_RESERVED_WORDS = List(
    "for",
    "public",
    "if",
    "else",
    "throw"
  )

  val TEST_PARAMETER_NAMES = Map(
    "class_" -> "getClass_"
  )
}

class JavaSyntaxTest extends AnyFreeSpec with Matchers {
  import JavaSyntaxTest._

  "Reserved word escaper should" - {
    "Escape reserved words" in {
      TEST_RESERVED_WORDS.foreach { word =>
        word.escapeReservedWord shouldBe (word + "_")
      }
    }

    "Not escape non-reserved words" in {
      List(
        "foo",
        "bar",
        "baz",
        "monkey",
        "cheese",
        "blah-moo",
        "aasdad2"
      ).foreach { word =>
        word.escapeReservedWord shouldBe word
      }
    }
  }

  "Identifier escaper should" - {
    "Escape identifiers that are reserved words" in {
      TEST_RESERVED_WORDS.foreach { word =>
        word.escapeIdentifier shouldBe (word + "_")
      }
    }

    "Escape identifiers that start with a number" in {
      List(
        "2",
        "3foo",
        "4-bar"
      ).foreach { word =>
        word.escapeIdentifier shouldBe ("_" + word)
      }
    }

    "Not escape identifiers that don't start with numbers" in {
      List(
        "f",
        "foo",
        "bar-baz",
        "quux"
      ).foreach { word =>
        word.escapeIdentifier shouldBe word
      }
    }

    "Escape properly with a bunch of random stuff thrown at it" in {
      new Random().alphanumeric
        .grouped(20)
        .take(500)
        .foreach { wordChars =>
          val word    = wordChars.mkString
          val escaped = word.escapeIdentifier
          if ("^[0-9]".r.findFirstMatchIn(word).isDefined) {
            escaped shouldBe ("_" + word)
          } else {
            escaped shouldBe word
          }
        }
    }
  }

  "safeParse should" - {
    "Produce a useful error string for a known error" in {
      val Failure(e) = Try(StaticJavaParser.parseName("dev.dashy-package-name.MyClass"))
      val result     = formatException("my prefix")(e)
      result shouldBe """my prefix: Unexpected "-" at character 10 (valid: <EOF>)"""
    }

    "Produce a useful error string for a custom complex error" in {
      val Failure(e) = Try(StaticJavaParser.parseClassOrInterfaceType(" }"))
      val result     = formatException("my prefix")(e)
      result shouldBe """my prefix: Unexpected "}" at character 2 (valid: "enum", "record", "strictfp", "yield", "requires", "to", "with", "open", "opens", "uses", "module", "exports", "provides", "transitive", <IDENTIFIER>)"""
    }
  }

  "Reserved method names should be escaped" in {
    TEST_PARAMETER_NAMES.foreach { case (name, escapedName) =>
      getterMethodNameForParameter(name) shouldBe escapedName
    }
  }
}
