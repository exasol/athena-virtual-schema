package com.exasol.adapter.dialects.athena;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.logging.VersionCollector;

/**
 * Factory for the Athena SQL dialect.
 */
public class AthenaSqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return AthenaSqlDialect.NAME;
    }

    @Override
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        return new AthenaSqlDialect(connectionFactory, properties);
    }

    @Override
    public String getSqlDialectVersion() {
        final VersionCollector versionCollector = new VersionCollector(
                "META-INF/maven/com.exasol/athena-virtual-schema/pom.properties");
        return versionCollector.getVersionNumber();
    }
}