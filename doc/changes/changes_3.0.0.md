# Virtual Schema for Athena 3.0.0, released 2026-05-28

Code name: Anonymous telemetry

## Summary

This release introduces anonymous feature-usage telemetry through `telemetry-java`. See the [documentation](https://github.com/exasol/telemetry-java/blob/main/doc/app-user-guide.md) for details about the collected data and how to opt out.

## Breaking Change

Starting with this release, this Virtual Schema no longer supports Exasol 7.1. The supported versions are the current release and the LTS release line `2025.1.x`.

## Features

* #31: Added anonymous feature-usage tracking

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:db-fundamentals-java:0.1.3` to `0.1.4`
* Updated `com.exasol:error-reporting-java:1.0.1` to `1.0.2`
* Updated `com.exasol:virtual-schema-common-jdbc:10.5.0` to `14.0.2`

### Test Dependency Updates

* Updated `com.exasol:virtual-schema-common-jdbc:10.5.0` to `14.0.2`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.14.1` to `3.19.4`
* Updated `org.hamcrest:hamcrest:2.2` to `3.0`
* Updated `org.junit.jupiter:junit-jupiter:5.9.2` to `5.14.4`
* Updated `org.mockito:mockito-junit-jupiter:5.3.0` to `5.23.0`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:2.0.5` to `2.0.7`
* Updated `com.exasol:project-keeper-maven-plugin:5.4.3` to `5.6.2`
* Updated `io.github.git-commit-id:git-commit-id-maven-plugin:9.0.2` to `10.0.0`
* Updated `org.apache.maven.plugins:maven-assembly-plugin:3.7.1` to `3.8.0`
* Updated `org.apache.maven.plugins:maven-compiler-plugin:3.14.1` to `3.15.0`
* Updated `org.apache.maven.plugins:maven-jar-plugin:3.4.2` to `3.5.0`
* Updated `org.apache.maven.plugins:maven-resources-plugin:3.3.1` to `3.5.0`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.5.4` to `3.5.5`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.19.1` to `2.21.0`
* Updated `org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988` to `5.5.0.6356`
