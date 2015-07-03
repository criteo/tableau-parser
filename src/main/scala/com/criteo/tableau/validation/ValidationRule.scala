package com.criteo.tableau.validation

import java.io.File

import scala.xml.{Elem, NodeSeq}

/**
 * Base class for the validation rules.
 * @tparam T Element to validate.
 */
abstract class ValidationRule[T] {
  /**
   * Returns whether the argument passes the rule.
   * @param arg The argument to validate.
   * @return [[Success]] if the argument is valid, [[Failure]] otherwise.
   */
  def validate(arg: T): ValidationStatus
}

/**
 * The file's size should be lower than some value.
 * @param maxSizeBytes The maximum file size, in bytes.
 */
case class MaxFileSize(maxSizeBytes: Long) extends ValidationRule[File] {
  override def validate(file: File): ValidationStatus = {
    val length: Long = file.length()

    if (length <= maxSizeBytes) Success
    else Failure(s"File is too big [$length > $maxSizeBytes] bytes")
  }
}

/**
 * The file's name should be lower than some value.
 * @param maxLength The maximum filename length.
 */
case class MaxFileNameLength(maxLength: Int) extends ValidationRule[File] {
  override def validate(file: File): ValidationStatus = {
    val length = file.getName.length

    if (length <= maxLength) Success
    else Failure(s"File name is too long [$length > $maxLength]")
  }
}

/**
 * A workbook should not have more than a specified number of dashboards.
 * @param threshold The maximum number of dashboards allowed, inclusive.
 */
case class MaximumDashboards(threshold: Int) extends ValidationRule[Elem] {
  override def validate(workbookXml: Elem): ValidationStatus = {
    val size = (workbookXml \ "dashboards" \ "dashboard").size

    if (size <= threshold) Success
    else Failure(s"Found [$size > $threshold] dashboards")
  }
}

/**
 * A workbook should have no extract in its datasources.
 */
case object NoExtract extends ValidationRule[Elem] {
  override def validate(workbookXml: Elem): ValidationStatus = {
    val datasourcesWithExtract = (workbookXml \ "datasources" \ "datasource") withFilter { datasource =>
      (datasource \ "extract").nonEmpty
    } map getName

    if (datasourcesWithExtract.isEmpty) Success
    else Failure(s"The following datasources have an extract [${datasourcesWithExtract.mkString(", ")}}]")
  }
}

/**
 * All connections of a workbook should have a valid class.
 * @param validClasses The list of valid classes.
 */
case class ConnectionsValidClass(validClasses: Array[String]) extends ValidationRule[Elem] {
  override def validate(workbookXml: Elem): ValidationStatus = {
    val wrongConnections = (workbookXml \ "datasources" \ "datasource") withFilter { datasource =>
      (datasource \ "connection" \ "@class").exists { connectionClass =>
        !validClasses.contains(connectionClass.toString())
      }
    } map getName

    if (wrongConnections.isEmpty) Success
    else Failure(s"The following datasources have an invalid connection [${wrongConnections.mkString(", ")}}]" +
      s" should be of type [${validClasses.mkString(", ")}]")
  }
}

/**
 * All dashboards of a workbook should have a fixed height and width.
 */
case object DashboardsHaveFixedSize extends ValidationRule[Elem] {
  def sizeIsFixed(size: NodeSeq): Boolean =
    size \ "@maxheight" == size \ "@minheight" && size \ "@maxwidth" == size \ "@minwidth"

  override def validate(workbookXml: Elem): ValidationStatus = {
    val dashboardSizes = (workbookXml \ "dashboards" \ "dashboard") map { dashboard =>
      val name = getName(dashboard)
      val sizes = dashboard \ "size"
      (name, sizes)
    }

    val automaticSize = dashboardSizes withFilter { case (name, sizes) =>
      sizes.isEmpty
    } map { case (name, sizes) =>
      name
    }

    val variableSize = dashboardSizes withFilter { case (name, sizes) =>
      !sizes.forall(sizeIsFixed)
    } map { case (name, sizes) =>
      name
    }

    if (automaticSize.isEmpty && variableSize.isEmpty)
      Success
    else if (automaticSize.isEmpty)
      Failure(s"The following dashboard have a variable size [${variableSize.mkString(", ")}]")
    else if (variableSize.isEmpty)
      Failure(s"The following dashboard have an automatic size [${automaticSize.mkString(", ")}]")
    else
      Failure(s"The following dashboard have a variable size [${variableSize.mkString(", ")}]" +
        s" and the following an automatic size [${automaticSize.mkString(", ")}]")
  }
}

/**
 * All sheets in a workbook should be hidden.
 */
case object NoVisibleSheet extends ValidationRule[Elem] {
  override def validate(workbookXml: Elem): ValidationStatus = {
    val visibleWorksheets = (workbookXml \ "windows" \ "window") withFilter { window =>
      (window \ "@class").head.toString() == "worksheet"
    } map getName

    if (visibleWorksheets.nonEmpty) Failure(s"Worksheets [${visibleWorksheets.mkString(", ")}] not hidden")
    else Success
  }
}

/**
 * All sheets in a workbook should be used in a dashboard.
 */
case object NoUnusedSheet extends ValidationRule[Elem] {
  override def validate(workbookXml: Elem): ValidationStatus = {
    val worksheetNames = getNames(workbookXml \ "worksheets" \ "worksheet")
    val zoneNames = getNames(workbookXml \ "dashboards" \ "dashboard" \ "zones" \\ "zone")

    val missingSheets: Seq[String] = worksheetNames.filterNot(zoneNames.contains(_))
    if (missingSheets.isEmpty) Success
    else Failure(s"The following sheets are not used in any dashboard [${missingSheets.mkString(", ")}]")
  }
}

/**
 * A dashboard should not have more than a specified number of sheets
 * @param threshold The maximum number of worksheets for a dashboard, inclusive.
 */
case class MaximumSheetsPerDashboard(threshold: Int) extends ValidationRule[Elem] {
  override def validate(workbookXml: Elem): ValidationStatus = {
    val bigDashboards = (workbookXml \ "dashboards" \ "dashboard") map { dashboard =>
      val size = getNames(dashboard \ "zones" \\ "zone").distinct.size
      val name = getName(dashboard)
      (name, size)
    } filter { case (name, size) =>
      size > threshold
    }

    if (bigDashboards.isEmpty) Success
    else Failure(s"The following dashboards have more than $threshold sheets [" +
      s"${bigDashboards.map { case (name, size) => s"$name: $size" }.mkString(", ")}]")
  }
}

/**
 * A dashboard should not have more than a specified number of quick filters (except if there are custom lists or
 * wildcards)
 * @param threshold The maximum number of quick filters for a dashboard, inclusive.
 */
case class MaximumQuickFiltersPerDashboard(threshold: Int) extends ValidationRule[Elem] {
  override def validate(workbookXml: Elem): ValidationStatus = {
    val bigDashboards = (workbookXml \ "dashboards" \ "dashboard") map { dashboard =>
      val zones = dashboard \ "zones" \\ "zone"
      val size = zones.count { zone =>
        (zone \ "@type").toString == "filter" &&
          (zone \ "@mode").toString != "typeinlist" &&
          (zone \ "@mode").toString != "pattern"
      }
      val name = getName(dashboard)
      (name, size)
    } filter  { case (name, size) =>
      size > threshold
    }

    if (bigDashboards.isEmpty) Success
    else Failure(s"The following dashboards have more than $threshold quick filters [" +
      s"${bigDashboards.map { case (name, size) => s"$name: $size" }.mkString(", ")}]")
  }
}
