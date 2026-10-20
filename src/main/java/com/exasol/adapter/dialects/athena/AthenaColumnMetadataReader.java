package com.exasol.adapter.dialects.athena;

import java.sql.Connection;
import java.sql.Types;
import java.util.Locale;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.IdentifierConverter;
import com.exasol.adapter.jdbc.BaseColumnMetadataReader;
import com.exasol.adapter.jdbc.JDBCTypeDescription;
import com.exasol.adapter.metadata.DataType;

/** Maps Athena JDBC metadata that differs from its result-set metadata. */
final class AthenaColumnMetadataReader extends BaseColumnMetadataReader {
    private static final int ATHENA_TIMESTAMP_PRECISION = 3;

    AthenaColumnMetadataReader(final Connection connection, final AdapterProperties properties, final ExaMetadata exaMetadata,
            final IdentifierConverter identifierConverter) {
        super(connection, properties, exaMetadata, identifierConverter);
    }

    @Override
    public DataType mapJdbcType(final JDBCTypeDescription description) {
        if ((description.getJdbcType() == Types.TIMESTAMP) || isTimestampWithTimeZone(description)) {
            return DataType.createTimestamp(false, ATHENA_TIMESTAMP_PRECISION);
        }
        return super.mapJdbcType(description);
    }

    static boolean isTimestampWithTimeZone(final JDBCTypeDescription description) {
        return (description.getJdbcType() == Types.TIMESTAMP_WITH_TIMEZONE) || isTimestampWithTimeZone(description.getTypeName());
    }

    static boolean isTimestampWithTimeZone(final String typeName) {
        if (typeName == null) {
            return false;
        }
        final String normalized = typeName.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return normalized.contains("timestamp") && normalized.contains("timezone");
    }
}
