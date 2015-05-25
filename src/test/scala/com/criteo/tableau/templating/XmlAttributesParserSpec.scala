package com.criteo.tableau.templating

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.util.parsing.input.{CharArrayReader, Reader}

/**
 * Spec for [[XmlAttributesParser]].
 */
@RunWith(classOf[JUnitRunner])
class XmlAttributesParserSpec extends FlatSpec with Matchers {

  def alwaysTrue: (AttributeKey, XmlScope) => Boolean = (_, _) => true

  /**
   * Convenience to convert our test inputs into a simple reader.
   * @param str The string to convert.
   * @return A simple reader around the string.
   */
  implicit def strToInput(str: String): Reader[Char] = new CharArrayReader(str.toCharArray)

  "parser" should "parse a valid xml and filter attributes" in {
    def condition(key: AttributeKey, scope: XmlScope): Boolean =
      List("valid1", "valid2").contains(key) && scope.nonEmpty && scope.head == "foo"

    val parser = XmlAttributesParser(condition)

    val xml =
      """<?xml version='1.0' encoding='utf-8' ?>
        |<a b='c'>
        |  <foo valid1='@value@' invalid = 'other' />
        |  <bar valid2='stuff'>
        |    hi i'm text <!-- i'm a comment -->
        |  </bar>
        |</a>
      """.stripMargin

    val finalState = parser(xml)
    finalState.attributes.length should equal(1)

    finalState.attributes.head match {
      case ScopedAttribute(xmlScope, Attribute(k, v, from, to)) =>
        k should equal("valid1")
        v should equal("@value@")
        xml.substring(from, to) should equal("@value@")
        xmlScope should equal(List("foo", "a"))
    }
  }

  "repWithState" should "match multiple nodes" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.repWithState(parser.emptyTag, State.empty)("<node1 a='b' /><node2 c='d' />") match {
      case parser.Success(State(attributes, tagStack), _) =>
        attributes should equal(List(
          ScopedAttribute(List("node1"), Attribute("a", "b", 10, 11)),
          ScopedAttribute(List("node2"), Attribute("c", "d", 25, 26))
        ))
        tagStack should equal(List.empty)
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "repWithState" should "throw an error if there are unclosed tags" in {
    val parser = XmlAttributesParser(alwaysTrue)
    parser.repWithState(state => parser.startTag(state) | parser.endTag(state), State.empty)("<a><b></b>") match {
      case parser.Error(_, _) =>
      case x => fail(s"Should get an error, got: $x")
    }
  }

  "startTag" should "parse a start tag" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.startTag(State.empty)("<foo bar='baz'>") match {
      case parser.Success(State(attributes, tagStack), _) =>
        attributes should equal(List(ScopedAttribute(List("foo"), Attribute("bar", "baz", 10, 13))))
        tagStack should equal(List("foo"))
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "startTag" should "ignore spaces" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.startTag(State.empty)("<foo bar='baz' >") match {
      case parser.Success(State(attributes, tagStack), _) =>
        attributes should equal(List(ScopedAttribute(List("foo"), Attribute("bar", "baz", 10, 13))))
        tagStack should equal(List("foo"))
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "emptyTag" should "parse an empty tag" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.emptyTag(State.empty)("<foo bar='baz'/>") match {
      case parser.Success(State(attributes, tagStack), _) =>
        attributes should equal(List(ScopedAttribute(List("foo"), Attribute("bar", "baz", 10, 13))))
        tagStack should equal(List.empty)
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "emptyTag" should "ignore spaces" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.emptyTag(State.empty)("<foo bar='baz' />") match {
      case parser.Success(State(attributes, tagStack), _) =>
        attributes should equal(List(ScopedAttribute(List("foo"), Attribute("bar", "baz", 10, 13))))
        tagStack should equal(List.empty)
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "endTag" should "unpop the parsed tag" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.endTag(State(Nil, List("foo")))("</foo>") match {
      case parser.Success(State(attributes, tagStack), _) =>
        attributes should equal(List.empty)
        tagStack should equal(List.empty)
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "endTag" should "throw an error if the tag is not correct" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.endTag(State(Nil, List("bar")))("</foo>") match {
      case parser.Error(_, _) =>
      case x => fail(s"Should get an error, got: $x")
    }
  }

  "endTag" should "throw an error if the stack is empty" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.endTag(State.empty)("</foo>") match {
      case parser.Error(_, _) =>
      case x => fail(s"Should get an error, got: $x")
    }
  }

  "comment" should "only match a comment" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.comment(State.empty)("<!-- comment --><tag/>") match {
      case parser.Success(newState, next) =>
        newState should equal(State.empty)
        next.offset should equal(16)
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "text" should "only match a text node" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.text(State.empty)("foo bar<tag/>") match {
      case parser.Success(newState, next) =>
        newState should equal(State.empty)
        next.offset should equal(7)
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "attributes" should "parse multiple attributes" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.attributes("a='b' c = 'd' e= \"f\"") match {
      case parser.Success(attributes, _) => attributes should equal(List(
        Attribute("a", "b", 3, 4),
        Attribute("c", "d", 11, 12),
        Attribute("e", "f", 18, 19)))
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "attribute" should "parse only one attribute" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.attribute("a='b' c='d'") match {
      case parser.Success(attr, next) =>
        attr should equal(Attribute("a", "b", 3, 4))
        next.offset should equal(5)
      case x => fail(s"Should get a success, got: $x")
    }
  }

  "attribute" should "ignore spaces around the '='" in {
    val parser = XmlAttributesParser(alwaysTrue)

    parser.attribute("a = 'bb'") match {
      case parser.Success(attr, _) =>
        attr should equal(Attribute("a", "bb", 5, 7))
      case x => fail(s"Should get a success, got: $x")
    }
  }
}
