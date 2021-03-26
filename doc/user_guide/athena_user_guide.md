# Athena SQL Dialect User Guide

The Athena SQL Dialect supports Amazon's [AWS Athena](https://aws.amazon.com/athena/), a managed service that lets you read files on S3 as if they were part of a relational database.

## Registering the JDBC Driver in EXAOperation

First download the [Athena JDBC driver](https://docs.aws.amazon.com/athena/latest/ug/connect-with-jdbc.html).

Now register the driver in EXAOperation:

1. Click "Software"
1. Switch to tab "JDBC Drivers"
1. Click "Browse..."
1. Select JDBC driver file
1. Click "Upload"
1. Click "Add"
1. In dialog "Add EXACluster JDBC driver" configure the JDBC driver (see below)

You need to specify the following settings when adding the JDBC driver via EXAOperation.

| Parameter | Value                                               |
|-----------|-----------------------------------------------------|
| Name      | `ATHENA`                                            |
| Main      | `com.simba.athena.jdbc.Driver`                      |
| Prefix    | `jdbc:awsathena:`                                   |
| Files     | `AthenaJDBC42_<JDBC driver version>.jar`            |

Please refer to the [documentation on configuring JDBC connections to Athena](https://docs.aws.amazon.com/athena/latest/ug/connect-with-jdbc.html) for details.

IMPORTANT: The latest Athena driver requires to **Disable Security Manager**.
It is necessary because JDBC driver requires Java permissions which we do not grant by default.

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
    %jar /buckets/<BFS service>/<bucket>/virtual-schema-dist-9.0.2-athena-2.0.0.jar;
    %jar /buckets/<BFS service>/<bucket>/AthenaJDBC42-<JDBC driver version>.jar;
/
;
```

## Defining a Named Connection

Define the connection to Athena as shown below. We recommend using TLS to secure the connection.

```sql
CREATE OR REPLACE CONNECTION ATHENA_CONNECTION
TO 'jdbc:awsathena://AwsRegion=<region>;S3OutputLocation=s3://<path to query results>'
USER '<access key ID>'
IDENTIFIED BY '<access key>';
```

## Creating a Virtual Schema

Below you see how an Athena Virtual Schema is created. Please note that you have to provide the name of the database in the property `SHEMA_NAME` since Athena simulates catalogs.

```sql
CREATE VIRTUAL SCHEMA <virtual schema name>
    USING ADAPTER.JDBC_ADAPTER
    WITH
    SQL_DIALECT = 'ATHENA'
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