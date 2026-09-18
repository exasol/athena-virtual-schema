# Developer Guide

## Executing Disabled Integration Tests

The integration tests for this repository are disabled, but it is possible to execute them locally because they require manual setup that is not yet automated for CI.

### Athena Integration Test Prerequisites

The Athena integration tests run a real Exasol container against a manually deployed, shared AWS fixture. They are
disabled by default and are not part of the GitHub Actions build.

1. Create `test_config.properties`:
   ```properties
   awsProfile = default
   awsRegion = eu-central-1
   cloudFormationStack = AthenaVirtualSchemaFixture
   exaOwner = <owner>
   exaDepartment = <department>
   exaCostCenter = <cost center>
   exaProject = <project>
   ```

2. Build and deploy the standalone CDK app in `ci-isolation/`:
   ```sh
   cd ci-isolation
   cdk deploy
   ```

### Starting Disabled Integration Test Locally

Run integration tests:
* Run `AthenaDialectIT` from your IDE or
* Run `mvn verify -DskipIntegrationTests=false -Dtest.coverage=false`

   Note that disabling coverage collection is required on Lima as a workaround for [udf-debugging-java#90](https://github.com/exasol/udf-debugging-java/issues/90).
