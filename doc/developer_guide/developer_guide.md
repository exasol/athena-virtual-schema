# Developer Guide

## Executing Disabled Integration Tests

The integration tests for this repository are disabled, but it is possible to execute them locally because they require manual setup that is not yet automated for CI.

### Starting Disabled Integration Test Locally

Run integration tests:
* Run `AthenaDialectIT` from your IDE or
* Run `mvn verify -DskipIntegrationTests=false`
