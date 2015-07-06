# tableau-parser
A parser for tableau workbook files.  It tries its best to unobtrusively parse tableau workbook files both for statistics gathering and search and replace operations.

## Statistics gathering
Tableau allows analysts to develop extremely complex analytic applications.  It can be useful to analyze the complexity of a given workbook in an attempt to understand its likely performance once published to a Tableau Server.

## Search and Replace
Unmodified Tableau workbooks are effectively impossible to store in source control repositories and put through an automated release process.

A typical example is that of database connection strings.
```
<datasources>
  <datasource caption='jobr_state_db (lta.jobr_state_db) (lta)' inline='true' name='vertica.42191.643693298600' version='9.0'>
    <connection class='vertica' dbname='bidata' odbc-connect-string-extras='' odbc-native-protocol='yes' one-time-sql='' port='5433' schema='lta' server='vertica22-am5.hpc.criteo.prod' username='j.coffey'>
      <relation name='jobr_state_db' table='[lta].[jobr_state_db]' type='table' />
```
We can quickly see that hardcoded usernames and database addresses are going to be difficult to deal with when pushing something into source control and/or preparing for release in a development/preproduction/production-type cycle.

You could get around the above without workbook parsing, of course.

To enable publishing workbooks against deployment-environment specific databases you could have different DNS servers with different DNS records based on deployment environment.  The downside is that you have to have well configured DNS servers for every possible DB target.  It's much more flexible to be able to manage this via a central configuration file.

To deal with the username problem, you could just give everyone the same username and password that works everywhere.  See if that flies with your security department ;).

## Usage guidelines
The best way to make use of it is via the tableau-maven-plugin

After a fresh pull from git:
```
$> mvn untemplate
```

To check complexity and compliance against rules:
```
$> mvn check
```

Before a push into git:
```
$> mvn template
```
