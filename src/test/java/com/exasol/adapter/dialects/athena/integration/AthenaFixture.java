package com.exasol.adapter.dialects.athena.integration;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

/** Provides AWS credentials and the manually deployed Athena fixture's configuration. */
final class AthenaFixture implements AutoCloseable {
    private final CloudFormationClient cloudFormation;
    private final AthenaClient athenaClient;
    private final AwsCredentials credentials;
    private final Map<String, String> outputs;

    private AthenaFixture(final CloudFormationClient cloudFormation,
            final AthenaClient athenaClient, final AwsCredentials credentials, final Map<String, String> outputs) {
        this.cloudFormation = cloudFormation;
        this.athenaClient = athenaClient;
        this.credentials = credentials;
        this.outputs = outputs;
    }

    static AthenaFixture create() {
        final TestConfig config = TestConfig.instance();
        final String region = config.getAwsRegion();
        final String stackName = config.getCloudFormationStack();
        final AwsCredentialsProvider credentialsProvider = config.getAwsCredentialsProvider();
        final AwsCredentials credentials = credentialsProvider.resolveCredentials();
        final CloudFormationClient cloudFormation = CloudFormationClient.builder().region(Region.of(region))
                .credentialsProvider(credentialsProvider).build();
        final AthenaClient athenaClient = AthenaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
        final Stack stack = cloudFormation.describeStacks(request -> request.stackName(stackName))
                .stacks().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("CloudFormation stack '" + stackName + "' does not exist."));
        final Map<String, String> outputs = stack.outputs().stream().collect(toUnmodifiableMap(Output::outputKey, Output::outputValue));
        requiredOutput(outputs, "Workgroup");
        requiredOutput(outputs, "Database");
        requiredOutput(outputs, "Table");
        requiredOutput(outputs, "OutputLocation");
        return new AthenaFixture(cloudFormation, athenaClient, credentials, outputs);
    }

    String connectionUrl() {
        final String sessionToken = this.credentials instanceof AwsSessionCredentials
                ? ";SessionToken=" + ((AwsSessionCredentials) this.credentials).sessionToken()
                : "";
        return "jdbc:athena://Region=" + TestConfig.instance().getAwsRegion() + ";Workgroup=" + output("Workgroup")
                + ";Catalog=AwsDataCatalog;Database=" + output("Database") + ";OutputLocation=" + output("OutputLocation")
                + sessionToken;
    }

    String getAccessKeyId() {
        return this.credentials.accessKeyId();
    }

    String getSecretAccessKey() {
        return this.credentials.secretAccessKey();
    }

    String getDatabase() {
        return output("Database");
    }

    String getTable() {
        return output("Table");
    }

    String createZonedTimestampIcebergTable() {
        final String tableName = "zoned_timestamp_" + UUID.randomUUID().toString().replace("-", "");
        final String tableLocation = output("OutputLocation") + "iceberg/" + tableName + "/";
        executeQuery("CREATE TABLE " + getDatabase() + "." + tableName
                + " WITH (table_type = 'ICEBERG', is_external = false, location = '" + tableLocation + "')"
                + " AS SELECT TIMESTAMP '2024-03-25 11:12:13.456 UTC' AS zoned_timestamp");
        return tableName;
    }

    void dropTable(final String tableName) {
        executeQuery("DROP TABLE IF EXISTS " + getDatabase() + "." + tableName);
    }

    boolean hasExecutedQueryContaining(final String fragment) {
        return this.athenaClient.listQueryExecutions(request -> request.workGroup(output("Workgroup")).maxResults(50))
                .queryExecutionIds().stream()
                .map(id -> this.athenaClient.getQueryExecution(request -> request.queryExecutionId(id)).queryExecution().query())
                .anyMatch(query -> query.contains(fragment));
    }

    private void executeQuery(final String query) {
        final String queryExecutionId = this.athenaClient.startQueryExecution(request -> request.queryString(query)
                .workGroup(output("Workgroup")).queryExecutionContext(context -> context.database(getDatabase())))
                .queryExecutionId();
        waitForQuery(queryExecutionId, query);
    }

    private void waitForQuery(final String queryExecutionId, final String query) {
        for (int attempt = 0; attempt < 300; attempt++) {
            final var status = this.athenaClient.getQueryExecution(request -> request.queryExecutionId(queryExecutionId))
                    .queryExecution().status();
            if (status.state() == QueryExecutionState.SUCCEEDED) {
                return;
            }
            if (status.state() == QueryExecutionState.FAILED || status.state() == QueryExecutionState.CANCELLED) {
                throw new IllegalStateException("Athena query failed: " + query + ". " + status.stateChangeReason());
            }
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Athena query: " + query, exception);
            }
        }
        throw new IllegalStateException("Athena query timed out: " + query);
    }

    private String output(final String name) {
        return requiredOutput(this.outputs, name);
    }

    private static String requiredOutput(final Map<String, String> outputs, final String name) {
        final String value = outputs.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("The Athena fixture stack is missing required output '" + name + "'.");
        }
        return value;
    }

    @Override
    public void close() {
        this.athenaClient.close();
        this.cloudFormation.close();
    }
}
