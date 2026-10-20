package com.exasol.adapter.dialects.athena;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;
import com.exasol.adapter.dialects.IdentifierCaseHandling;
import com.exasol.adapter.jdbc.JDBCTypeDescription;
import com.exasol.adapter.metadata.DataType;

@ExtendWith(MockitoExtension.class)
class AthenaColumnMetadataReaderTest {
    @Mock
    private Connection connection;
    @Mock
    private ExaMetadata exaMetadata;

    private AthenaColumnMetadataReader reader;

    @BeforeEach
    void beforeEach() {
        when(this.exaMetadata.getDatabaseVersion()).thenReturn("8.0.0");
        this.reader = new AthenaColumnMetadataReader(this.connection, AdapterProperties.emptyProperties(), this.exaMetadata,
                new BaseIdentifierConverter(IdentifierCaseHandling.INTERPRET_AS_UPPER,
                        IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE));
    }

    @ParameterizedTest
    @ValueSource(ints = { Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE })
    void mapsTimestampJdbcTypesToMillisecondPrecision(final int jdbcType) {
        assertThat(this.reader.mapJdbcType(new JDBCTypeDescription(jdbcType, 0, 6, 0, "timestamp")),
                equalTo(DataType.createTimestamp(false, 3)));
    }

    @Test
    void mapsZonedTimestampTypeNameToMillisecondPrecision() {
        assertThat(this.reader.mapJdbcType(new JDBCTypeDescription(Types.OTHER, 0, 6, 0, "timestamp with time zone")),
                equalTo(DataType.createTimestamp(false, 3)));
    }

    @Test
    void delegatesNonTimestampTypesToBaseReader() {
        assertThat(this.reader.mapJdbcType(new JDBCTypeDescription(Types.VARCHAR, 0, 12, 0, "varchar")),
                equalTo(DataType.createVarChar(12, DataType.ExaCharset.UTF8)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "timestamp with time zone", "timestamp(6) with timezone", "TIMESTAMP WITH TIME ZONE" })
    void recognizesZonedTimestampTypeNames(final String typeName) {
        assertThat(AthenaColumnMetadataReader.isTimestampWithTimeZone(typeName), is(true));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "timestamp", "time with time zone", "varchar" })
    void doesNotRecognizeOtherTypeNames(final String typeName) {
        assertThat(AthenaColumnMetadataReader.isTimestampWithTimeZone(typeName), is(false));
    }
}
