package com.criteo.tableau

package object templating {
  /**
   * The key of an attribute.
   */
  type AttributeKey = String

  /**
   * The scope, as a list of xml tags. The current tag is at the head, and the root of the document is the last element.
   */
  type XmlScope = List[String]
}
