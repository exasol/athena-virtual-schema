package com.exasol.adapter.dialects.athena.integration;

import java.sql.Statement;

import com.exasol.dbbuilder.dialects.exasol.VirtualSchema;

final class TestFixture implements AutoCloseable {
    private final AthenaFixture athena;
    private final ExasolContainerFixture exasol;

    private TestFixture(final AthenaFixture athena, final ExasolContainerFixture exasol) {
        this.athena = athena;
        this.exasol = exasol;
    }

    static TestFixture create() {
        final AthenaFixture athena = AthenaFixture.create();
        try {
            return new TestFixture(athena, new ExasolContainerFixture(AthenaJdbcDriverManager.download()));
        } catch (final RuntimeException exception) {
            athena.close();
            throw exception;
        }
    }

    VirtualSchema createVirtualSchema() {
        return this.exasol.createVirtualSchema(this.athena);
    }

    Statement statement() {
        return this.exasol.statement();
    }

    String table() {
        return this.athena.getTable();
    }

    String createZonedTimestampIcebergTable() {
        return this.athena.createZonedTimestampIcebergTable();
    }

    void dropTable(final String tableName) {
        this.athena.dropTable(tableName);
    }

    boolean hasExecutedQueryContaining(final String fragment) {
        return this.athena.hasExecutedQueryContaining(fragment);
    }

    @Override
    public void close() {
        try {
            this.exasol.close();
        } finally {
            this.athena.close();
        }
    }
}
