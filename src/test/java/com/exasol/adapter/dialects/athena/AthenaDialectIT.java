package com.exasol.adapter.dialects.athena;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exasol.dbbuilder.dialects.exasol.VirtualSchema;

/** Full-path tests for Exasol, the adapter, Athena JDBC 3.x, and the shared AWS fixture. */
class AthenaDialectIT {
    private static AthenaVirtualSchemaFixture fixture;
    private static VirtualSchema virtualSchema;

    @BeforeAll
    static void beforeAll() {
        fixture = AthenaVirtualSchemaFixture.create();
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
        try (ResultSet result = fixture.statement().executeQuery("SELECT name FROM " + virtualSchema.getName() + ".\""
                + fixture.table() + "\" WHERE active = TRUE ORDER BY id DESC LIMIT 2")) {
            final List<String> names = new ArrayList<>();
            while (result.next()) {
                names.add(result.getString(1));
            }
            assertThat(names, contains("gamma", "alpha"));
        }
        assertThat(fixture.hasExecutedQueryContaining("LIMIT 2"), is(true));
    }

    @Test
    void pushesDownScalarAndAggregateExpressions() throws SQLException {
        try (ResultSet result = fixture.statement().executeQuery("SELECT COUNT(*), SUM(amount), UPPER(MIN(name)) FROM "
                + virtualSchema.getName() + ".\"" + fixture.table() + "\"")) {
            assertThat(result.next(), org.hamcrest.Matchers.is(true));
            assertThat(result.getLong(1), org.hamcrest.Matchers.is(3L));
            assertThat(result.getBigDecimal(2), org.hamcrest.Matchers.comparesEqualTo(new java.math.BigDecimal("60.75")));
            assertThat(result.getString(3), org.hamcrest.Matchers.is("ALPHA"));
        }
        assertThat(fixture.hasExecutedQueryContaining("UPPER"), is(true));
    }
}
