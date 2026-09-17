# Developer Guide

## Executing Disabled Integration Tests

The integration tests for this repository are disabled, but it is possible to execute them locally because they require manual setup that is not yet automated for CI.

### Athena Integration Test Prerequisites

The Athena integration tests run a real Exasol container against a manually deployed, shared AWS fixture. They are
disabled by default and are not part of the GitHub Actions build.

1. Build and deploy the standalone CDK app in [`ci-isolation/`](../../ci-isolation/README.md).
2. Create `test_config.properties` based on this template:
   ```properties
   awsProfile = athena-virtual-schema-it
   awsRegion = eu-central-1
   cloudFormationStack = AthenaVirtualSchemaFixture
   ```

### Starting Disabled Integration Test Locally

Run integration tests:
* Run `AthenaDialectIT` from your IDE or
* Run `mvn verify -DskipIntegrationTests=false`
