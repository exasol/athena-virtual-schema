# Athena SQL Dialect User Guide

The Athena SQL Dialect supports Amazon's [AWS Athena](https://aws.amazon.com/athena/), a managed service that lets you read files on S3 as if they were part of a relational database.

## Telemetry

This virtual schema uses `telemetry-java` to send anonymous feature-usage events.

For details on what is collected and how to disable telemetry, see the [documentation](https://github.com/exasol/telemetry-java/blob/main/doc/app-user-guide.md).

## Uploading the JDBC Driver to Exasol BucketFS

1. Download the [Athena JDBC driver](https://docs.aws.amazon.com/athena/latest/ug/connect-with-jdbc.html).
2. Upload the driver to BucketFS, see [BucketFS documentation](https://docs.exasol.com/db/latest/administration/on-premise/bucketfs/accessfiles.htm).

    Hint: Put the driver into folder `default/drivers/jdbc/` to register it for [ExaLoader](#registering-the-jdbc-driver-for-exaloader), too.

## Registering the JDBC driver for ExaLoader

In order to enable the ExaLoader to fetch data from the external database you must register the driver for ExaLoader as described in the [Installation procedure for JDBC drivers](https://github.com/exasol/docker-db/#installing-custom-jdbc-drivers).
1. ExaLoader expects the driver in BucketFS folder `default/drivers/jdbc`.

   If you uploaded the driver for UDF to a different folder, then you need to [upload](#uploading-the-jdbc-driver-to-exasol-bucketfs) the driver again.
2. Additionally  you need to create file `settings.cfg` and [upload](#uploading-the-jdbc-driver-to-exasol-bucketfs) it to the same folder in BucketFS:

   ```properties
   DRIVERNAME=ATHENA
   JAR=AthenaJDBC42.jar
   DRIVERMAIN=com.simba.athena.jdbc.Driver
   PREFIX=jdbc:awsathena:
   NOSECURITY=YES
   FETCHSIZE=100000
   INSERTSIZE=-1
   
   ```
   Ensure that the file ends with a trailing newline.

## Installing the Adapter Script

Upload the latest available release of [Athena Virtual Schema](https://github.com/exasol/athena-virtual-schema/releases) to Bucket FS.

Then create a schema to hold the adapter script.

```sql
CREATE SCHEMA ADAPTER;
```

The SQL statement below creates the adapter script, defines the Java class that serves as entry point and tells the UDF framework where to find the libraries (JAR files) for Virtual Schema and database driver.

```sql
CREATE OR REPLACE JAVA ADAPTER SCRIPT ADAPTER.JDBC_ADAPTER AS
    %scriptclass com.exasol.adapter.RequestDispatcher;
    %jar /buckets/<BFS service>/<bucket>/virtual-schema-dist-14.0.2-athena-3.0.0.jar;
    %jar /buckets/<BFS service>/<bucket>/AthenaJDBC42.jar;
/
;
```

## Defining a Named Connection

Define the connection to Athena as shown below. We also recommend using TLS to secure the connection.

```sql
CREATE OR REPLACE CONNECTION ATHENA_CONNECTION
TO 'jdbc:awsathena://AwsRegion=<region>;S3OutputLocation=s3://<path to query results>'
USER '<access key ID>'
IDENTIFIED BY '<access key>';
```

Please refer to the [documentation on configuring JDBC connections to Athena](https://docs.aws.amazon.com/athena/latest/ug/connect-with-jdbc.html) for details.

For the connection troubleshooting refer to the [AWS documentation](https://aws.amazon.com/premiumsupport/knowledge-center/). Search for Amazon Athena on the page.

## Creating a Virtual Schema

Below you see how an Athena Virtual Schema is created. Please note that you have to provide the name of the database in the property `SHEMA_NAME` since Athena simulates catalogs.

```sql
CREATE VIRTUAL SCHEMA <virtual schema name>
    USING ADAPTER.JDBC_ADAPTER
    WITH
    CONNECTION_NAME = 'ATHENA_CONNECTION'
    SCHEMA_NAME = '<database name>';
```

## Data Types Conversion

| Athena Data Type   | Supported | Converted Exasol Data Type| Known limitations
|--------------------|-----------|---------------------------|-------------------
| ARRAY              |  ×        |                           |
| BOOLEAN            |  ✓        | BOOLEAN                   |
| BIGINT             |  ✓        | DECIMAL                   |
| BINARY             |  ×        |                           |
| CHAR               |  ✓        | CHAR                      |
| DATE               |  ✓        | DATE                      |
| DECIMAL            |  ✓        | DECIMAL                   |
| DOUBLE             |  ✓        | DOUBLE                    |
| FLOAT              |  ✓        | DOUBLE                    |
| INTEGER            |  ✓        | DECIMAL(19,0)             |
| MAP                |  ×        |                           |
| SMALLINT           |  ✓        | DECIMAL                   |
| STRING*            |  ✓        | VARCHAR                   |
| STRUCT             |  ×        |                           |
| TIMESTAMP          |  ✓        | TIMESTAMP                 |
| TINYINT            |  ✓        | DECIMAL                   |
| VARCHAR            |  ✓        | VARCHAR                   |

* Please be aware that the recommended Simba JDBC driver returns 255 as a default length of the String data type. It means that if you have a longer String value, the Exasol database would throw an Exception. To avoid this, you can specify a String length in the connection string:

```
CREATE OR REPLACE CONNECTION ATHENA_CONNECTION
TO 'jdbc:awsathena://AwsRegion=<region>;S3OutputLocation=s3://<path to query results>;StringColumnLength=2000000'
USER '<access key ID>'
IDENTIFIED BY '<access key>';
```

In this example we used the maximum length of the Exasol Varchar datatype.

## Troubleshooting 

### SELECT Query Hangs and Returns Timeout

If you created a Virtual Schema successfully, but a SELECT query runs forever without any result, check the following things:

* Verify that you disabled a security manager in the JDBC driver installed in EXAoperation.
* Verify that Exasol can receive outgoing connections from AWS on port 443 and 444. For that, ssh into Exasol node and run netcat commands:

   ```shell
   nc -v athena.eu-west-1.amazonaws.com 443
   nc -v athena.eu-west-1.amazonaws.com 444
   ```
   
   `athena.eu-west-1.amazonaws.com` is a public endpoint. If you use a private VPC endpoint with Athena, please specify it instead of public one. If a port is not opened, you will see output like this:
   
   ```shell
   [root@n0011 ~]# nc -v athena.eu-west-1.amazonaws.com 444
   Ncat: Version 7.50 ( https://nmap.org/ncat )
   Ncat: Connection to 52.49.83.92 failed: Connection timed out.
   Ncat: Trying next address...
   Ncat: Connection to 52.209.65.135 failed: Connection timed out.
   Ncat: Trying next address...
   ...
   ```
   
   In this case, you need to enable outbound traffic on the port (usually, it is blocked by your firewall).

* Enable Athena JDBC driver logs and check them: maybe there is a missing permission. To enable the logs, you need to modify a connection string. Append this line to the connection string, recreate a connection and run a query again:

   ```
   LogLevel=5;LogPath=/tmp/athena/
   ```
   
   You can find the logs in the `/tmp/athena/` directory on the Exasol Node.

* See also: https://aws.amazon.com/premiumsupport/knowledge-center/athena-connection-timeout-jdbc-odbc-driver/

### You get an error: Value null at 'workGroup' failed to satisfy constraint: Member must not be null

**Solution**: use the JDBC driver 2.0.23 or later. Some old driver versions had a bug leading to this error.
See the driver's [changelog](https://s3.cn-north-1.amazonaws.com.cn/athena-downloads-cn/drivers/JDBC/SimbaAthenaJDBC-2.0.23.1000/docs/release-notes.txt) file for additional information.

### If Athena database/table/column name has special characters except underscore, Virtual Schema throws an error

**Solution**: This is an expected behavior. We validate on the Virtual Schema side that Athena identifiers only contain supported characters following this rule: `Special characters other than underscore (_) are not supported.`
See [the official AWS documentation](https://docs.aws.amazon.com/athena/latest/ug/tables-databases-columns-names.html) for more information.
