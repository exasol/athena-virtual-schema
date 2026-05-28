package com.exasol.adapter.dialects.athena;

import com.exasol.adapter.dialects.*;
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
    public SqlDialect createSqlDialect(final JDBCAdapterContext context) {
        return new AthenaSqlDialect(context);
    }

    @Override
    public String getSqlDialectVersion() {
        final VersionCollector versionCollector = new VersionCollector(
                "META-INF/maven/com.exasol/athena-virtual-schema/pom.properties");
        return versionCollector.getVersionNumber();
    }

    @Override
    public String getAdapterProjectShortTag() {
        return "VSATHENA";
    }
}
