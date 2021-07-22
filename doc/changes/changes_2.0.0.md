# Virtual Schema for Athena 2.0.0, released 2021-??-??

Code name: Removed `SQL_DIALECT` property

## Summary

The `SQL_DIALECT` property used when executing a `CREATE VIRTUAL SCHEMA` from the Exasol database is obsolete from this version. Please, do not provide this property anymore.

## Documentation

* #4: Described String data type.
* #8: Improved user guide, replaced outdated information.
* #11: Added a troubleshooting guide.

## Refactoring

* #13: Switched from Travis to GitHub build.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:0.2.0` to `0.4.0`
* Updated `com.exasol:virtual-schema-common-jdbc:8.0.0` to `9.0.3`

### Test Dependency Updates

* Updated `com.exasol:virtual-schema-common-jdbc:8.0.0` to `9.0.3`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.5` to `3.7`
* Updated `org.junit.jupiter:junit-jupiter:5.7.0` to `5.7.2`
* Updated `org.mockito:mockito-junit-jupiter:3.6.28` to `3.11.2`

### Plugin Dependency Updates

* Added `com.exasol:error-code-crawler-maven-plugin:0.4.0`
* Updated `com.exasol:project-keeper-maven-plugin:0.4.2` to `0.10.0`
* Added `io.github.zlika:reproducible-build-maven-plugin:0.13`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.7` to `2.8.1`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.5` to `0.8.7`