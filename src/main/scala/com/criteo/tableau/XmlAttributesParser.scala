package com.criteo.tableau

import com.criteo.tableau.XmlAttributesParser.{AttributeKey, XmlScope}

import scala.annotation.tailrec
import scala.collection.immutable.Stack
import scala.util.parsing.combinator.RegexParsers

/**
 * State used during parsing, storing all valid attributes, and the current scope.
 * @param attributes Valid attributes.
 * @param scope      Current scope in the Xml document.
 */
case class State(attributes: List[ScopedAttribute], scope: XmlScope)

object State {
  val empty = State(Nil, Stack.empty)
}

/**
 * A Xml attribute, with the current Xml scope enriching it.
 * @param scope     The scope at which the attribute is found.
 * @param attribute The attribute.
 */
case class ScopedAttribute(scope: XmlScope, attribute: Attribute)

/**
 * A Xml attribute inside a Xml document.
 * @param key        The attribute key.
 * @param value      The attribute value.
 * @param fromOffset The starting offset of the value inside the Xml document.
 * @param toOffset   The end offset of the value inside the Xml document.
 */
case class Attribute(key: AttributeKey, value: String, fromOffset: Int, toOffset: Int)

object XmlAttributesParser {
  /**
   * The key of an attribute.
   */
  type AttributeKey = String

  /**
   * The scope, as a stack of xml tags. The current tag is first, and the root of the document is last.
   */
  type XmlScope = Stack[String]
}

/**
 * A Xml parser, keeping all attributes satisfying a filter.
 * @param keyPositionFilter Should return true if the attribute with a specific key at a specific scope should be kept.
 */
case class XmlAttributesParser(keyPositionFilter: (AttributeKey, XmlScope) => Boolean) extends RegexParsers {

  override val skipWhitespace = true

  def apply(input: String): State = parseAll(parser, input) match {
    case Success(result, _) => result
    case failure: NoSuccess => scala.sys.error(failure.toString)
  }

  val parser: Parser[State] = version ~> repWithState(anyNode, State.empty)

  /**
   * The xml version tag at the beginning of the file.
   */
  lazy val version = "<?xml" ~ attributes ~ "?>"

  /**
   * Repeatedly parses until failure, updating the state at each successful parsing, starting from an empty
   * [[com.criteo.tableau.State State]].
   * @param p The parser to repeat.
   */
  def repWithState(p: (State) => Parser[State], initialState: State): Parser[State] = Parser { in =>

    @tailrec def applyp(currentState: State, in0: Input): ParseResult[State] = {
      p(currentState)(in0) match {
        case Success(newState, rest) =>
          applyp(newState, rest)
        case e: Error => e
        case _ =>
          if (currentState.scope.isEmpty)
            Success(currentState, in0)
          else
            Error(s"Unclosed tags: [${currentState.scope.reverse.mkString(",")}]", in0)
      }
    }

    applyp(initialState, in)
  }

  /**
   * Any node that we can find inside a xml document.
   * @param state The current state.
   */
  def anyNode(state: State): Parser[State] =
    startTag(state) | endTag(state) | emptyTag(state) | text(state) | comment(state)

  /**
   * Parses a comment node, with no change to the current state.
   * @param state The current state.
   */
  def comment(state: State): Parser[State] = commentExpr map { _ => state }

  lazy val commentExpr = "<!--" ~ (("""[^\-]""".r | ("-" ~ not("->"))) *) ~ "-->"

  /**
   * Parses a text node, with no change to the current state.
   * @param state The current state.
   */
  def text(state: State): Parser[State] = textExpr map { _ => state }

  lazy val textExpr: Parser[String] = "[^<]+".r

  /**
   * Parses a start tag node, like `&lt;xxx k1="v1" k2="v2"&gt;`, adds its attributes to the current state, and its name
   * to the stack.
   * @param state The current state.
   */
  def startTag(state: State): Parser[State] = startTagExpr map {
    case tag ~ attrList =>
      val currentPosition = state.scope.push(tag)
      val newAttributes = attrList filter {
        attr => keyPositionFilter(attr.key, currentPosition)
      } map {
        ScopedAttribute(currentPosition, _)
      }
      state.copy(attributes = state.attributes ::: newAttributes, scope = currentPosition)
  }

  lazy val startTagExpr: Parser[String ~ List[Attribute]] = "<" ~> name ~ attributes <~ ">"

  /**
   * Parses an end tag node, like `&lt;/xxx&gt;`, and go back up the stack.
   * @param state The current state.
   */
  def endTag(state: State): Parser[State] = endTagExpr flatMap {
    case tag =>
      if (state.scope.isEmpty)
        err(s"Encountered end tag [$tag] with empty stack")
      else if (state.scope.head != tag)
        err(s"Encountered end tag [$tag] with stack head [${state.scope.head}]")
      else
        success(state.copy(scope = state.scope.pop))
  }

  lazy val endTagExpr: Parser[String] = "</" ~> name <~ ">"

  /**
   * Parses an empty tag node, like `&lt;xxx k1="v1" k2="v2"/&gt;`, and adds its attributes to the current state.
   * @param state The current state.
   */
  def emptyTag(state: State): Parser[State] = emptyTagExpr map {
    case tag ~ attrList =>
      val currentPosition = state.scope.push(tag)
      val newAttributes = attrList filter {
        attr => keyPositionFilter(attr.key, currentPosition)
      } map {
        ScopedAttribute(currentPosition, _)
      }
      state.copy(attributes = state.attributes ::: newAttributes)
  }

  lazy val emptyTagExpr: Parser[String ~ List[Attribute]] = "<" ~> name ~ attributes <~ "/>"

  /**
   * Parses a node name or attribute key.
   */
  def name: Parser[String] = """(:|\w)(\-|\.|\d|:|\w)*""".r

  /**
   * A list of attributes.
   */
  lazy val attributes: Parser[List[Attribute]] = attribute *

  /**
   * Parses an attribute, and returns an [[com.criteo.tableau.Attribute Attribute]] containing the key and value
   * of the attribute, and the offsets around the value.
   */
  lazy val attribute: Parser[Attribute] = Parser { in =>
    attributePair(in) match {
      case Success(key ~ value, rest) =>
        Success(Attribute(key, value, rest.offset - value.length - 1, rest.offset - 1), rest)
      case n: NoSuccess => n
    }
  }

  /**
   * Parses an attribute, and returns the key-value pair.
   */
  lazy val attributePair: Parser[String ~ String] = (name <~ """\s*=\s*""".r) ~ quotedString

  /**
   * A single or double quoted string.
   */
  lazy val quotedString: Parser[String] = xQuotedString('\"') | xQuotedString('\'')

  /**
   * A string delimited by quotes.
   * @param quote the quote character.
   */
  def xQuotedString(quote: Char): Parser[String] = quote ~> s"[^$quote]*".r <~ quote
}