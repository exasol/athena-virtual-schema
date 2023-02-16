# Athena SQL Dialect User Guide

The Athena SQL Dialect supports Amazon's [AWS Athena](https://aws.amazon.com/athena/), a managed service that lets you read files on S3 as if they were part of a relational database.

## Registering the JDBC Driver in EXAOperation

First download the [Athena JDBC driver](https://docs.aws.amazon.com/athena/latest/ug/connect-with-jdbc.html).

Now register the driver in EXAOperation:

1. Click "Software"
1. Switch to the tab "JDBC Drivers"
1. Click "Add"
1. In dialog "Add EXACluster JDBC driver" configure the JDBC driver (see table below)

   | Parameter                | Value                          |
   |--------------------------|--------------------------------|
   | Driver Name              | `ATHENA`                       |
   | Main Class               | `com.simba.athena.jdbc.Driver` |
   | Prefix                   | `jdbc:awsathena:`              |
   | Disable Security Manager | true                           |

1. Click "Add"
1. Select a round radio button of the created driver
1. Click "Browse..."
1. Select JDBC driver file (`AthenaJDBC42.jar`)
1. Click "Upload"

IMPORTANT: The latest Athena driver requires to **Disable Security Manager** because JDBC driver requires Java permissions which we do not grant by default.

## Uploading the JDBC Driver to BucketFS

1. [Create a bucket in BucketFS](https://docs.exasol.com/administration/on-premise/bucketfs/create_new_bucket_in_bucketfs_service.htm)
1. Upload the driver to BucketFS

This step is necessary since the UDF container the adapter runs in has no access to the JDBC drivers installed via EXAOperation but it can access BucketFS.

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
    %jar /buckets/<BFS service>/<bucket>/virtual-schema-dist-10.1.0-athena-2.0.1.jar;
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
