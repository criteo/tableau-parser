package com.criteo.tableau.templating

case class XmlAttributeReplacer(replacementRule: (AttributeKey, XmlScope) => Option[String]) {

  /**
   * Combines this replacer with another one.
   * Any attribute not replaced by this replacer is candidate for the other one.
   * @param otherReplacer The other replacer to combine this one with.
   * @return A combination of this replacer and the other one, where this one takes priority.
   */
  def orElse(otherReplacer: XmlAttributeReplacer): XmlAttributeReplacer =
    XmlAttributeReplacer((key, scope) => replacementRule(key, scope).orElse(otherReplacer.replacementRule(key, scope)))

  /**
   * Replace all attribute values in a xml document according to the rule.
   * @param xmlDocument The document to process.
   * @return The result of applying the replacement rule on the document.
   */
  def replace(xmlDocument: String): String = {
    val parser = XmlAttributesParser(replacementRule(_, _).nonEmpty)
    val sb = parser(xmlDocument).attributes.foldRight(new StringBuilder(xmlDocument)) { (attribute, sb) =>
      replacementRule(attribute.attribute.key, attribute.scope) map {
        sb.replace(attribute.attribute.fromOffset, attribute.attribute.toOffset, _)
      } getOrElse sb
    }
    sb.toString()
  }
}

object XmlAttributeReplacer {

  val connectionReplacer: XmlAttributeReplacer = XmlAttributeReplacer(connectionRule)

  private def connectionRule(key: AttributeKey, xmlScope: XmlScope): Option[String] =
    if (xmlScope == List("connection", "datasource", "datasources", "workbook"))
      key match {
        case "dbname" => Some("%%DBNAME%%")
        case "odbc-connect-string-extras" => Some("%%ODBCEXTRA%%")
        case "port" => Some("%%PORT%%")
        case "server" => Some("%%SERVER%%")
        case "username" => Some("%%USERNAME%%")
        case _ => None
      }
    else
      None
}
