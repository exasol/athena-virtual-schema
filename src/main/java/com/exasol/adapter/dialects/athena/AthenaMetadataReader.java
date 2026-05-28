package com.exasol.adapter.dialects.athena;

import java.sql.Connection;
import java.util.Set;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.*;

/**
 * Metadata reader that reads AWS-Athena-specific database metadata.
 */
public class AthenaMetadataReader extends AbstractRemoteMetadataReader {
    /**
     * Create a new instance of the {@link AthenaMetadataReader}.
     *
     * @param connection  JDBC connection to the remote data source
     * @param properties  user-defined adapter properties
     * @param exaMetadata Exasol metadata
     */
    public AthenaMetadataReader(final Connection connection, final AdapterProperties properties, final ExaMetadata exaMetadata) {
        super(connection, properties, exaMetadata);
    }

    @Override
    public Set<String> getSupportedTableTypes() {
        return RemoteMetadataReaderConstants.ANY_TABLE_TYPE;
    }

    @Override
    protected IdentifierConverter createIdentifierConverter() {
        return new BaseIdentifierConverter(IdentifierCaseHandling.INTERPRET_AS_UPPER,
                IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE);
    }

    @Override
    protected ColumnMetadataReader createColumnMetadataReader() {
        return new BaseColumnMetadataReader(this.connection, this.properties, this.exaMetadata, this.identifierConverter);
    }

    @Override
    protected TableMetadataReader createTableMetadataReader() {
        return new BaseTableMetadataReader(this.connection, this.columnMetadataReader, this.properties, this.exaMetadata, this.identifierConverter);
    }
}
