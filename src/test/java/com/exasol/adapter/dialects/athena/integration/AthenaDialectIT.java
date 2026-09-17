package com.exasol.adapter.dialects.athena.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.junit.jupiter.api.*;

import com.exasol.dbbuilder.dialects.exasol.VirtualSchema;

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
}
