# Virtual Schema for Athena 2.0.0, released 2021-??-??

Code name: Removed `SQL_DIALECT` property

## Summary

The `SQL_DIALECT` property used when executing a `CREATE VIRTUAL SCHEMA` from the Exasol database is obsolete from this version. Please, do not provide this property anymore.

## Documentation

* #4: Described String data type.
* #8: Improved user guide, replaced outdated information.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:0.2.0` to `0.4.0`
* Updated `com.exasol:virtual-schema-common-jdbc:8.0.0` to `9.0.2`

### Test Dependency Updates

* Updated `com.exasol:virtual-schema-common-jdbc:8.0.0` to `9.0.2`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.5` to `3.5.5`
* Updated `org.junit.jupiter:junit-jupiter:5.7.0` to `5.7.1`
* Updated `org.mockito:mockito-junit-jupiter:3.6.28` to `3.8.0`

### Plugin Dependency Updates

* Added `com.exasol:error-code-crawler-maven-plugin:0.1.1`
* Updated `com.exasol:project-keeper-maven-plugin:0.4.2` to `0.6.0`
* Added `io.github.zlika:reproducible-build-maven-plugin:0.13`