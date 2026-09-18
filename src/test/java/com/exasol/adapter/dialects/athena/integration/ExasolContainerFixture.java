package com.exasol.adapter.dialects.athena.integration;

import static com.exasol.dbbuilder.dialects.exasol.AdapterScript.Language.JAVA;

import java.nio.file.Path;
import java.sql.*;
import java.util.Map;

import com.exasol.bucketfs.Bucket;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolService;
import com.exasol.dbbuilder.dialects.exasol.*;
import com.exasol.udfdebugging.UdfTestSetup;

/** Owns the ephemeral Exasol container and its BucketFS driver/adapter deployment. */
final class ExasolContainerFixture implements AutoCloseable {
    private static final String ADAPTER_JAR = "virtual-schema-dist-14.0.5-athena-3.0.1.jar";
    private static final Path ADAPTER_JAR_PATH = Path.of("target", ADAPTER_JAR);
    private static final String SCHEMA_NAME = "ATHENA_IT";
    private final ExasolContainer<? extends ExasolContainer<?>> container;
    private final Connection connection;
    private final Statement statement;
    private final UdfTestSetup udfTestSetup;
    private final ExasolObjectFactory objectFactory;
    private final AdapterScript adapterScript;
    private int virtualSchemaCounter;

    ExasolContainerFixture(final AthenaJdbcDriverManager jdbcDriver) {
        try {
            this.container = new ExasolContainer<>()
                    .withRequiredServices(ExasolService.BUCKETFS, ExasolService.UDF).withReuse(true);
            this.container.start();
            final Bucket bucket = this.container.getDefaultBucket();
            bucket.uploadFile(ADAPTER_JAR_PATH, ADAPTER_JAR);
            installAthenaDriver(jdbcDriver);
            this.connection = this.container.createConnection("");
            this.statement = this.connection.createStatement();
            this.udfTestSetup = new UdfTestSetup(this.container.getHostIp(), this.container.getDefaultBucket(), this.connection);
            this.objectFactory = new ExasolObjectFactory(this.container.createConnection(""),
                    ExasolObjectConfiguration.builder().withJvmOptions(this.udfTestSetup.getJvmOptions()).build());
            final ExasolSchema schema = this.objectFactory.createSchema(SCHEMA_NAME);
            this.adapterScript = schema.createAdapterScript("ATHENA_ADAPTER", JAVA,
                    "%scriptclass com.exasol.adapter.RequestDispatcher;\n"
                            + "%jar /buckets/bfsdefault/default/" + ADAPTER_JAR + ";\n"
                            + "%jar /buckets/bfsdefault/default/drivers/jdbc/" + jdbcDriver.fileName() + ";\n");
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to prepare the Exasol integration-test container.", exception);
        }
    }

    VirtualSchema createVirtualSchema(final AthenaFixture athena) {
        final ConnectionDefinition definition = this.objectFactory.createConnectionDefinition("ATHENA_CONNECTION_" + this.virtualSchemaCounter,
                athena.connectionUrl(), athena.getAccessKeyId(), athena.getSecretAccessKey());
        return this.objectFactory.createVirtualSchemaBuilder("ATHENA_VIRTUAL_SCHEMA_" + this.virtualSchemaCounter++)
                .adapterScript(this.adapterScript)
                .connectionDefinition(definition)
                .addProperties(Map.of("SCHEMA_NAME", athena.getDatabase()))
                .build();
    }

    Statement statement() {
        return this.statement;
    }

    private void installAthenaDriver(final AthenaJdbcDriverManager jdbcDriver) {
        this.container.getDriverManager().install(jdbcDriver.asExasolDriver());
    }

    @Override
    public void close() {
        try {
            this.statement.close();
            this.udfTestSetup.close();
            this.connection.close();
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to close the Exasol test connection.", exception);
        } finally {
            this.container.stop();
        }
    }
}
