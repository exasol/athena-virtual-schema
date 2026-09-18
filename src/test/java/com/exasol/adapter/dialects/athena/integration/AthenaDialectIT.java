package com.exasol.adapter.dialects.athena.integration;

import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.junit.jupiter.api.*;

import com.exasol.dbbuilder.dialects.exasol.VirtualSchema;

/**
 * Integration tests for the Athena dialect.
 * <p>
 * AWS test fixture already contains an Athena database with table containing data from {@code ci-isolation/src/main/resources/fixture/athena_test_data.csv}.
 * </p>
 */
class AthenaDialectIT {
    private static TestFixture fixture;
    private static VirtualSchema virtualSchema;

    @BeforeAll
    static void beforeAll() {
        fixture = TestFixture.create();
        virtualSchema = fixture.createVirtualSchema();
    }

    @AfterAll
    static void afterAll() {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    void importsMetadataAndPushesDownProjectionFilterOrderAndLimit() throws SQLException {
        assertThat(fixture.statement().executeQuery("SELECT \"name\" FROM " + virtualSchema.getName()
                + ".\"" + fixture.table() + "\" WHERE \"active\" = TRUE ORDER BY \"id\" DESC LIMIT 2"),
                table().row("gamma").row("alpha").matches());
        assertThat(fixture.hasExecutedQueryContaining("LIMIT 2"), is(true));
    }

    @Test
    void pushesDownScalarAndAggregateExpressions() throws SQLException {
        assertThat(fixture.statement().executeQuery("SELECT COUNT(*), SUM(\"amount\"), UPPER(MIN(\"name\")) FROM "
                + virtualSchema.getName() + ".\"" + fixture.table() + "\""),
                table().row(3L, new BigDecimal("60.75"), "ALPHA").matches());
        assertThat(fixture.hasExecutedQueryContaining("UPPER"), is(true));
    }

    @Test
    void loadsTimestamps() throws SQLException {
        assertThat(fixture.statement().executeQuery("SELECT \"created_at\" FROM " + virtualSchema.getName()
                + ".\"" + fixture.table() + "\" WHERE \"id\" = 1"),
                table().withUtcCalendar().row(Timestamp.valueOf("2023-01-11 15:15:14")).matches());
    }

    @Test
    void reproducesFailureForTimestampWithTimeZoneFromIcebergTable() {
        final String tableName = fixture.createZonedTimestampIcebergTable();
        try {
            final VirtualSchema icebergVirtualSchema = fixture.createVirtualSchema();
            final SQLException exception = assertThrows(SQLException.class,
                    () -> fixture.statement().executeQuery("SELECT \"zoned_timestamp\" FROM " + icebergVirtualSchema.getName()
                            + ".\"" + tableName + "\""));
            assertThat(exception.getMessage(), startsWith("ETL-5402: JDBC-Client-Error: JDBC SQL Type for column=0 (starting at 0) value=2014 is unknown."));
        } finally {
            fixture.dropTable(tableName);
        }
    }
}
