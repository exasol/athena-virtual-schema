package com.exasol.adapter.dialects.athena;

import java.util.Map;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.athena.AthenaClient;

/** Provides AWS credentials and the manually deployed Athena fixture's configuration. */
final class AthenaFixture implements AutoCloseable {
    private final AwsCredentialsProvider credentialsProvider;
    private final CloudFormationClient cloudFormation;
    private final AthenaClient athenaClient;
    private final AwsCredentials credentials;
    private final Map<String, String> outputs;

    private AthenaFixture(final AwsCredentialsProvider credentialsProvider, final CloudFormationClient cloudFormation,
            final AthenaClient athenaClient, final AwsCredentials credentials, final Map<String, String> outputs) {
        this.credentialsProvider = credentialsProvider;
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
        final AthenaClient athenaClient = AthenaClient.builder().region(Region.of(region)).credentialsProvider(credentialsProvider)
                .build();
        final Stack stack = cloudFormation.describeStacks(request -> request.stackName(stackName)).stacks().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("CloudFormation stack '" + stackName + "' does not exist."));
        final Map<String, String> outputs = stack.outputs().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(output -> output.outputKey(), output -> output.outputValue()));
        requiredOutput(outputs, "Workgroup");
        requiredOutput(outputs, "Database");
        requiredOutput(outputs, "Table");
        requiredOutput(outputs, "OutputLocation");
        return new AthenaFixture(credentialsProvider, cloudFormation, athenaClient, credentials, outputs);
    }

    String connectionUrl() {
        final String sessionToken = this.credentials instanceof AwsSessionCredentials
                ? ";SessionToken=" + ((AwsSessionCredentials) this.credentials).sessionToken()
                : "";
        return "jdbc:athena://Region=" + TestConfig.instance().getAwsRegion() + ";Workgroup=" + output("Workgroup")
                + ";Catalog=AwsDataCatalog;Database=" + output("Database") + ";OutputLocation=" + output("OutputLocation")
                + sessionToken;
    }

    String accessKeyId() {
        return this.credentials.accessKeyId();
    }

    String secretAccessKey() {
        return this.credentials.secretAccessKey();
    }

    String database() {
        return output("Database");
    }

    String table() {
        return output("Table");
    }

    boolean hasExecutedQueryContaining(final String fragment) {
        return this.athenaClient.listQueryExecutions(request -> request.workGroup(output("Workgroup")).maxResults(50))
                .queryExecutionIds().stream()
                .map(id -> this.athenaClient.getQueryExecution(request -> request.queryExecutionId(id)).queryExecution().query())
                .anyMatch(query -> query.contains(fragment));
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
