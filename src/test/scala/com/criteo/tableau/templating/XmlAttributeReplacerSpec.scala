package com.criteo.tableau.templating

import org.scalatest.{FlatSpec, Matchers}

/**
 * Spec for [[XmlAttributeReplacer]].
 */
class XmlAttributeReplacerSpec extends FlatSpec with Matchers {

  "replace" should "replace valid attribute values" in {
    def replacement(key: AttributeKey, xmlScope: XmlScope): Option[String] =
      if (key == "key1") Some("AAA")
      else None

    val xml =
      """<?xml version='1.0' encoding='utf-8' ?>
        |<a>
        |  <foo key1 = 'value1' />
        |  <bar key2 = 'value2' />
        |</a>
      """.stripMargin

    XmlAttributeReplacer(replacement).replace(xml) should equal(
      """<?xml version='1.0' encoding='utf-8' ?>
        |<a>
        |  <foo key1 = 'AAA' />
        |  <bar key2 = 'value2' />
        |</a>
      """.stripMargin
    )
  }

  "orElse" should "replaced attribute values in order" in {
    def replacement1(key: AttributeKey, xmlScope: XmlScope): Option[String] =
      if (key == "key1") Some("AAA")
      else None

    def replacement2(key: AttributeKey, xmlScope: XmlScope): Option[String] =
      Some("BBB")

    val xml =
      """<?xml version='1.0' encoding='utf-8' ?>
        |<a>
        |  <foo key1 = 'value1' />
        |  <bar key2 = 'value2' />
        |</a>
      """.stripMargin

    val replacer1 = XmlAttributeReplacer(replacement1)
    val replacer2 = XmlAttributeReplacer(replacement2)

    replacer1.orElse(replacer2).replace(xml) should equal(
      """<?xml version='1.0' encoding='utf-8' ?>
        |<a>
        |  <foo key1 = 'AAA' />
        |  <bar key2 = 'BBB' />
        |</a>
      """.stripMargin
    )

    replacer2.orElse(replacer1).replace(xml) should equal(
      """<?xml version='1.0' encoding='utf-8' ?>
        |<a>
        |  <foo key1 = 'BBB' />
        |  <bar key2 = 'BBB' />
        |</a>
      """.stripMargin
    )
  }

  "connectionReplacer" should "replace connection attributes in a Tableau workbook" in {
    val xml =
      """<?xml version='1.0' encoding='utf-8' ?>
        |<workbook version='8.2' xml:base='https://tableau-server.example.com' xmlns:user='http://www.tableausoftware.com/xml/user'>
        |  <datasources>
        |    <datasource caption='Vertica (Prod)' inline='true' name='vertica.41891.470850972219' version='8.2'>
        |      <connection class='vertica' dbname='dwh' expected-driver-version='7.1' odbc-connect-string-extras='ConnectionLoadBalance=1;SSLMode=disable;BackupServerNode=vertica-02.node' odbc-native-protocol='yes' one-time-sql='' port='5433' server='vertica01-node' server-oauth='' username='f.bar' workgroup-auth-mode='prompt'>
        |        <relation name='TableauSQL' type='text'>SELECT &#13;&#10;    user_name,&#13;&#10;    (all_roles LIKE &apos;%analyst%&apos;) AS is_analyst&#13;&#10;FROM users&#13;&#10;WHERE user_name LIKE &apos;%.%&apos;&#13;&#10;ORDER BY user_name</relation>
        |        ...
        |      </connection>
        |    </datasource>
        |  </datasources>
        |</workbook>
      """.stripMargin

    XmlAttributeReplacer.connectionReplacer.replace(xml) should equal(
      """<?xml version='1.0' encoding='utf-8' ?>
        |<workbook version='8.2' xml:base='https://tableau-server.example.com' xmlns:user='http://www.tableausoftware.com/xml/user'>
        |  <datasources>
        |    <datasource caption='Vertica (Prod)' inline='true' name='vertica.41891.470850972219' version='8.2'>
        |      <connection class='vertica' dbname='%%DBNAME%%' expected-driver-version='7.1' odbc-connect-string-extras='%%ODBCEXTRA%%' odbc-native-protocol='yes' one-time-sql='' port='%%PORT%%' server='%%SERVER%%' server-oauth='' username='%%USERNAME%%' workgroup-auth-mode='prompt'>
        |        <relation name='TableauSQL' type='text'>SELECT &#13;&#10;    user_name,&#13;&#10;    (all_roles LIKE &apos;%analyst%&apos;) AS is_analyst&#13;&#10;FROM users&#13;&#10;WHERE user_name LIKE &apos;%.%&apos;&#13;&#10;ORDER BY user_name</relation>
        |        ...
        |      </connection>
        |    </datasource>
        |  </datasources>
        |</workbook>
      """.stripMargin
    )
  }
}
