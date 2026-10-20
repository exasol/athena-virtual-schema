package com.exasol.adapter.dialects.athena;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.BaseTableMetadataReader;
import com.exasol.adapter.jdbc.JDBCTypeDescription;
import com.exasol.adapter.metadata.DataType;

@ExtendWith(MockitoExtension.class)
class AthenaMetadataReaderTest {
    private AthenaMetadataReader reader;
    @Mock
    Connection connectionMock;
    @Mock
    ExaMetadata exaMetadataMock;

    @BeforeEach
    void beforeEach() {
        when(this.exaMetadataMock.getDatabaseVersion()).thenReturn("3.2.1");
        this.reader = new AthenaMetadataReader(this.connectionMock, AdapterProperties.emptyProperties(), this.exaMetadataMock);
    }

    @Test
    void testGetTableMetadataReader() {
        assertThat(this.reader.getTableMetadataReader(), instanceOf(BaseTableMetadataReader.class));
    }

    @Test
    void testGetColumnMetadataReader() {
        assertThat(this.reader.getColumnMetadataReader(), instanceOf(AthenaColumnMetadataReader.class));
    }

    @Test
    void mapsTimestampToMillisecondPrecision() {
        final AthenaColumnMetadataReader columnReader = (AthenaColumnMetadataReader) this.reader.getColumnMetadataReader();

        assertThat(columnReader.mapJdbcType(new JDBCTypeDescription(Types.TIMESTAMP, 0, 0, 0, "timestamp")),
                equalTo(DataType.createTimestamp(false, 3)));
    }

    @Test
    void testGetSupportedTableTypes() {
        assertThat(this.reader.getSupportedTableTypes(), emptyIterableOf(String.class));
    }

    @Test
    void testCreateIdentifierConverter() {
        final IdentifierConverter converter = this.reader.getIdentifierConverter();
        assertAll(() -> assertThat(converter, instanceOf(BaseIdentifierConverter.class)),
                () -> assertThat(converter.getQuotedIdentifierHandling(),
                        equalTo(IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE)),
                () -> assertThat(converter.getUnquotedIdentifierHandling(),
                        equalTo(IdentifierCaseHandling.INTERPRET_AS_UPPER)));
    }
}
