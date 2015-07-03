package com.criteo.tableau.validation

import java.io.File

import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

/**
 * Spec for [[ValidationRule]].
 */
@RunWith(classOf[JUnitRunner])
class ValidationRuleSpec extends FlatSpec with Matchers with MockitoSugar {

  "MaxFileSize" should "succeed for a small file" in {
    val file = mock[File]
    Mockito.when(file.length()).thenReturn(10)
    MaxFileSize(1024).validate(file).isSuccess should equal(true)
  }

  it should "fail for a big file" in {
    val file = mock[File]
    Mockito.when(file.length()).thenReturn(2000)
    MaxFileSize(1024).validate(file).isSuccess should equal(false)
  }

  "MaxFileNameLength" should "succeed for a short filename" in {
    val file = new File("some_file.twb")
    MaxFileNameLength(20).validate(file).isSuccess should equal(true)
  }

  it should "fail for a long filename" in {
    val file = new File("a_very_long_file_name_like_really_long.twb")
    MaxFileNameLength(20).validate(file).isSuccess should equal(false)
  }

  "MaximumDashboards" should "succeed if there are fewer nodes" in {
    val workbook =
      <workbook>
        <dashboards>
          <dashboard name='Dashboard1'>
          </dashboard>
          <dashboard name='Dashboard2'>
          </dashboard>
          <dashboard name='Dashboard3'>
          </dashboard>
        </dashboards>
      </workbook>
    MaximumDashboards(3).validate(workbook).isSuccess should equal(true)
  }

  it should "fail if there are too many nodes" in {
    val workbook =
      <workbook>
        <dashboards>
          <dashboard name='Dashboard1'>
          </dashboard>
          <dashboard name='Dashboard2'>
          </dashboard>
          <dashboard name='Dashboard3'>
          </dashboard>
          <dashboard name='Dashboard4'>
          </dashboard>
        </dashboards>
      </workbook>
    MaximumDashboards(3).validate(workbook).isSuccess should equal(false)
  }

  "NoExtract" should "succeed if all datasources are live" in {
    val workbook =
      <workbook>
        <datasources>
          <datasource name='Source1'>
            <connection class='vertica'>
            </connection>
          </datasource>
          <datasource hasconnection='false' name='Parameters'/>
          <datasource name='Source2'>
            <connection class='excel-direct'>
            </connection>
          </datasource>
        </datasources>
      </workbook>
    NoExtract.validate(workbook).isSuccess should equal(true)
  }

  it should "fail if there is an extract" in {
    val workbook =
      <workbook>
        <datasources>
          <datasource name='Source1'>
            <connection class='vertica'>
            </connection>
            <extract>
            </extract>
          </datasource>
        </datasources>
      </workbook>
    NoExtract.validate(workbook).isSuccess should equal(false)
  }

  "ConnectionsValidClass" should "succeed if all connections are valid" in {
    val workbook =
      <workbook>
        <datasources>
          <datasource name='Source1'>
            <connection class='vertica'>
            </connection>
          </datasource>
          <datasource hasconnection='false' name='Parameters'/>
          <datasource name='Source2'>
            <connection class='vertica'>
            </connection>
          </datasource>
        </datasources>
      </workbook>
    ConnectionsValidClass(Array("vertica")).validate(workbook).isSuccess should equal(true)
  }

  it should "fail if there is an invalid connection" in {
    val workbook =
      <workbook>
        <datasources>
          <datasource name='Source1'>
            <connection class='vertica'>
            </connection>
          </datasource>
          <datasource name='Source2'>
            <connection class='sqlserver'>
            </connection>
          </datasource>
        </datasources>
      </workbook>
    ConnectionsValidClass(Array("vertica")).validate(workbook).isSuccess should equal(false)
  }

  "DashboardsHaveFixedSize" should "succeed if all dashboards have fixed size" in {
    val workbook =
      <workbook>
        <dashboards>
          <dashboard name='Dashboard1'>
            <size minheight='100' maxheight='100' minwidth='100' maxwidth='100'/>
          </dashboard>
        </dashboards>
      </workbook>
    DashboardsHaveFixedSize.validate(workbook).isSuccess should equal(true)
  }

  it should "fail if a dashboard has an automatic size" in {
    val workbook =
      <workbook>
        <dashboards>
          <dashboard name='Dashboard1'>
          </dashboard>
        </dashboards>
      </workbook>
    DashboardsHaveFixedSize.validate(workbook).isSuccess should equal(false)
  }

  it should "fail if a dashboard has variable height" in {
    val workbook =
      <workbook>
        <dashboards>
          <dashboard name='Dashboard1'>
            <size minheight='100' maxheight='200' minwidth='100' maxwidth='100'/>
          </dashboard>
        </dashboards>
      </workbook>
    DashboardsHaveFixedSize.validate(workbook).isSuccess should equal(false)
  }

  it should "fail if a dashboard has variable width" in {
    val workbook =
      <workbook>
        <dashboards>
          <dashboard name='Dashboard1'>
            <size minheight='100' maxheight='100' minwidth='100' maxwidth='200'/>
          </dashboard>
        </dashboards>
      </workbook>
    DashboardsHaveFixedSize.validate(workbook).isSuccess should equal(false)
  }

  "NoVisibleSheet" should "succeed if there are no visible sheets" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
        </worksheets>
        <windows>
          <window class='hidden-worksheet' name='Worksheet1'>
          </window>
          <window class='hidden-worksheet' name='Worksheet2'>
          </window>
        </windows>
      </workbook>
    NoVisibleSheet.validate(workbook).isSuccess should equal(true)
  }

  it should "fail if there is a visible sheet" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
        </worksheets>
        <windows>
          <window class='worksheet' name='Worksheet1'>
          </window>
          <window class='hidden-worksheet' name='Worksheet2'>
          </window>
        </windows>
      </workbook>
    NoVisibleSheet.validate(workbook).isSuccess should equal(false)
  }

  "NoUnusedSheet" should "succeed if all sheets are in a dashboard" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
        </worksheets>
        <dashboards>
          <dashboard name='Dashboard1'>
            <zones>
              <zone>
                <zone>
                </zone>
                <zone name='Worksheet1'>
                </zone>
              </zone>
            </zones>
          </dashboard>
          <dashboard name='Dashboard2'>
            <zones>
              <zone>
                <zone>
                  <zone name='Worksheet2'>
                  </zone>
                </zone>
              </zone>
              <zone>
              </zone>
            </zones>
          </dashboard>
        </dashboards>
      </workbook>
    NoUnusedSheet.validate(workbook).isSuccess should equal(true)
  }

  it should "fail if there is an unused sheet" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
        </worksheets>
        <dashboards>
          <dashboard name='Dashboard1'>
            <zones>
              <zone>
                <zone>
                </zone>
                <zone name='Worksheet1'>
                </zone>
              </zone>
            </zones>
          </dashboard>
          <dashboard name='Dashboard2'>
            <zones>
              <zone>
                <zone>
                  <zone name='Worksheet1'>
                  </zone>
                </zone>
              </zone>
            </zones>
          </dashboard>
        </dashboards>
      </workbook>
    NoUnusedSheet.validate(workbook).isSuccess should equal(false)
  }

  "MaximumSheetsPerDashboard" should "succeed if there are fewer worksheets used in dashboards" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
          <worksheet name='Worksheet3'>
          </worksheet>
        </worksheets>
        <dashboards>
          <dashboard name='Dashboard1'>
            <zones>
              <zone>
                <zone>
                </zone>
                <zone name='Worksheet1'>
                </zone>
                <zone name='Worksheet2'>
                </zone>
              </zone>
              <zone name='Worksheet3'>
              </zone>
            </zones>
          </dashboard>
          <dashboard name='Dashboard2'>
            <zones>
              <zone>
                <zone>
                  <zone name='Worksheet2'>
                  </zone>
                </zone>
              </zone>
              <zone>
                <zone name='Worksheet1'>
                </zone>
              </zone>
            </zones>
          </dashboard>
        </dashboards>
      </workbook>
    MaximumSheetsPerDashboard(3).validate(workbook).isSuccess should equal(true)
  }

  it should "fail if there are too many worksheets used in dashboards" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
          <worksheet name='Worksheet3'>
          </worksheet>
        </worksheets>
        <dashboards>
          <dashboard name='Dashboard1'>
            <zones>
              <zone>
                <zone>
                </zone>
                <zone name='Worksheet1'>
                </zone>
                <zone name='Worksheet2'>
                </zone>
              </zone>
              <zone name='Worksheet3'>
              </zone>
            </zones>
          </dashboard>
          <dashboard name='Dashboard2'>
            <zones>
              <zone>
                <zone>
                  <zone name='Worksheet2'>
                  </zone>
                </zone>
              </zone>
              <zone>
                <zone name='Worksheet1'>
                </zone>
              </zone>
            </zones>
          </dashboard>
        </dashboards>
      </workbook>
    MaximumSheetsPerDashboard(2).validate(workbook).isSuccess should equal(false)
  }

  "MaximumQuickFiltersPerDashboard" should "succeed if there are fewer quick filters present in dashboards" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
        </worksheets>
        <dashboards>
          <dashboard name='Dashboard1'>
            <zones>
              <zone>
                <zone name='Worksheet2' type="filter" mode="pattern">
                </zone>
                <zone>
                  <zone name='Worksheet2' type="filter">
                  </zone>
                </zone>
                <zone name='Worksheet1'>
                </zone>
                <zone name='Worksheet3' type="filter" mode="typeinlist">
                </zone>
                <zone name='Worksheet2'>
                </zone>
                <zone name='Worksheet2' type="filter">
                </zone>
              </zone>
              <zone name='Worksheet3'>
              </zone>
            </zones>
          </dashboard>
          <dashboard name='Dashboard2'>
            <zones>
              <zone>
                <zone>
                  <zone name='Worksheet2'>
                  </zone>
                </zone>
              </zone>
              <zone>
                <zone name='Worksheet1'>
                </zone>
                <zone name='Worksheet1' type="filter">
                </zone>
              </zone>
            </zones>
          </dashboard>
        </dashboards>
      </workbook>
    MaximumQuickFiltersPerDashboard(2).validate(workbook).isSuccess should equal(true)
  }

  it should "fail if there are too many quick filters present in dashboards" in {
    val workbook =
      <workbook>
        <worksheets>
          <worksheet name='Worksheet1'>
          </worksheet>
          <worksheet name='Worksheet2'>
          </worksheet>
        </worksheets>
        <dashboards>
          <dashboard name='Dashboard1'>
            <zones>
              <zone>
                <zone name='Worksheet2' type="filter" mode="dropdown">
                </zone>
                <zone>
                  <zone name='Worksheet2' type="filter">
                  </zone>
                </zone>
                <zone name='Worksheet1'>
                </zone>
                <zone name='Worksheet3' type="filter" mode="typeinlist">
                </zone>
                <zone name='Worksheet2'>
                </zone>
                <zone name='Worksheet2' type="filter">
                </zone>
              </zone>
              <zone name='Worksheet3'>
              </zone>
            </zones>
          </dashboard>
          <dashboard name='Dashboard2'>
            <zones>
              <zone>
                <zone>
                  <zone name='Worksheet2'>
                  </zone>
                </zone>
              </zone>
              <zone>
                <zone name='Worksheet1'>
                </zone>
                <zone name='Worksheet1' type="filter">
                </zone>
              </zone>
            </zones>
          </dashboard>
        </dashboards>
      </workbook>
    MaximumQuickFiltersPerDashboard(2).validate(workbook).isSuccess should equal(false)
  }

}
