package com.exasol.adapter.dialects.athena;

import static com.exasol.adapter.AdapterProperties.*;
import static com.exasol.adapter.capabilities.AggregateFunctionCapability.*;
import static com.exasol.adapter.capabilities.LiteralCapability.*;
import static com.exasol.adapter.capabilities.MainCapability.*;
import static com.exasol.adapter.capabilities.PredicateCapability.*;
import static com.exasol.adapter.capabilities.ScalarFunctionCapability.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect.NullSorting;
import com.exasol.adapter.dialects.SqlDialect.StructureElementSupport;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.jdbc.RemoteMetadataReaderException;
import com.exasol.adapter.properties.DataTypeDetection;
import com.exasol.adapter.properties.TableCountLimit;

@ExtendWith(MockitoExtension.class)
class AthenaSqlDialectTest {
    private AthenaSqlDialect dialect;
    @Mock
    private ConnectionFactory connectionFactoryMock;

    @BeforeEach
    void beforeEach() {
        this.dialect = new AthenaSqlDialect(this.connectionFactoryMock, AdapterProperties.emptyProperties());
    }

    @Test
    void testGetName() {
        assertThat(this.dialect.getName(), equalTo("ATHENA"));
    }

    @Test
    void testGetMainCapabilities() {
        assertThat(this.dialect.getCapabilities().getMainCapabilities(),
                containsInAnyOrder(SELECTLIST_PROJECTION, SELECTLIST_EXPRESSIONS, FILTER_EXPRESSIONS,
                        AGGREGATE_SINGLE_GROUP, AGGREGATE_GROUP_BY_COLUMN, AGGREGATE_GROUP_BY_EXPRESSION,
                        AGGREGATE_GROUP_BY_TUPLE, AGGREGATE_HAVING, ORDER_BY_COLUMN, ORDER_BY_EXPRESSION, LIMIT));
    }

    @Test
    void testGetLiteralCapabilitiesLiteral() {
        assertThat(this.dialect.getCapabilities().getLiteralCapabilities(),
                containsInAnyOrder(NULL, BOOL, DATE, TIMESTAMP, TIMESTAMP_UTC, DOUBLE, EXACTNUMERIC, STRING, INTERVAL));
    }

    @Test
    void testGetPredicateCapabilities() {
        assertThat(this.dialect.getCapabilities().getPredicateCapabilities(), containsInAnyOrder(AND, OR, NOT, EQUAL,
                NOTEQUAL, LESS, LESSEQUAL, LIKE, REGEXP_LIKE, BETWEEN, IS_NULL, IS_NOT_NULL));
    }

    @Test
    void testGetScalarFunctionCapabilities() {
        assertThat(this.dialect.getCapabilities().getScalarFunctionCapabilities(),
                containsInAnyOrder(CAST, ABS, CEIL, ACOS, ASIN, ATAN, ATAN2, COS, COSH, DEGREES, EXP, FLOOR, LN, LOG,
                        MOD, POWER, RADIANS, RAND, ROUND, SIGN, SIN, SQRT, TAN, TANH, TRUNC, BIT_AND, BIT_NOT, BIT_OR,
                        BIT_XOR, CHR, CONCAT, LENGTH, LOWER, LPAD, LTRIM, REPLACE, REVERSE, RPAD, RTRIM, SUBSTR, TRIM,
                        UPPER, CURRENT_DATE, CURRENT_TIMESTAMP, DATE_TRUNC, MINUTE, SECOND, DAY, MONTH, WEEK, YEAR,
                        REGEXP_REPLACE, HASH_MD5, HASH_SHA1));
    }

    @Test
    void testGetAggregateFunctionCapabilities() {
        assertThat(this.dialect.getCapabilities().getAggregateFunctionCapabilities(),
                containsInAnyOrder(COUNT, COUNT_STAR, SUM, MIN, MAX, AVG, STDDEV, STDDEV_POP, STDDEV_SAMP, VARIANCE,
                        VAR_POP, VAR_SAMP, APPROXIMATE_COUNT_DISTINCT));
    }

    @Test
    void testSupportsJdbcCatalogs() {
        assertThat(this.dialect.supportsJdbcCatalogs(), equalTo(StructureElementSupport.SINGLE));
    }

    @Test
    void testSupportsJdbcSchemas() {
        assertThat(this.dialect.supportsJdbcSchemas(), equalTo(StructureElementSupport.MULTIPLE));
    }

    @Test
    void testRequiresCatalogQualifiedTableNames() {
        assertThat(this.dialect.requiresCatalogQualifiedTableNames(null), equalTo(false));
    }

    @Test
    void testRequiresSchemaQualifiedTableNames() {
        assertThat(this.dialect.requiresSchemaQualifiedTableNames(null), equalTo(true));
    }

    @Test
    void testGetDefaultNullSorting() {
        assertThat(this.dialect.getDefaultNullSorting(), equalTo(NullSorting.NULLS_SORTED_AT_END));
    }

    @CsvSource({ "tableName, \"tableName\"", //
            "table123, \"table123\"", //
            "_table, \"_table\"", //
            "123table, \"123table\"", //
            "table_name, \"table_name\"" })
    @ParameterizedTest
    void testApplyQuote(final String unquoted, final String quoted) {
        assertThat(this.dialect.applyQuote(unquoted), equalTo(quoted));
    }

    @Test
    void testGetLiteralStringNull() {
        assertThat(this.dialect.getStringLiteral(null), equalTo("NULL"));
    }

    @ValueSource(strings = { "ab:'ab'", "a'b:'a''b'", "a''b:'a''''b'", "'ab':'''ab'''" })
    @ParameterizedTest
    void testGetLiteralString(final String definition) {
        assertThat(this.dialect.getStringLiteral(definition.substring(0, definition.indexOf(':'))),
                equalTo(definition.substring(definition.indexOf(':') + 1)));
    }

    @Test
    void testMetadataReaderClass(@Mock final Connection connectionMock) throws SQLException {
        when(this.connectionFactoryMock.getConnection()).thenReturn(connectionMock);
        assertThat(this.dialect.createRemoteMetadataReader(), instanceOf(AthenaMetadataReader.class));
    }

    @Test
    void testCreateMetadataReaderFails(@Mock final Connection connectionMock) throws SQLException {
        when(this.connectionFactoryMock.getConnection()).thenThrow(new SQLException("mock"));
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                this.dialect::createRemoteMetadataReader);
        assertThat(exception.getMessage(),
                equalTo("E-VSATHENA-2: Unable to create Athena remote metadata reader. Caused by: 'mock'"));
    }

    @Test
    void testGetSupportedProperties() {
        assertThat(this.dialect.getSupportedProperties(),
                containsInAnyOrder(CONNECTION_NAME_PROPERTY, CATALOG_NAME_PROPERTY, SCHEMA_NAME_PROPERTY,
                        TABLE_FILTER_PROPERTY, EXCLUDED_CAPABILITIES_PROPERTY, DEBUG_ADDRESS_PROPERTY,
                        LOG_LEVEL_PROPERTY, DataTypeDetection.STRATEGY_PROPERTY, TableCountLimit.MAXTABLES_PROPERTY));
    }
}