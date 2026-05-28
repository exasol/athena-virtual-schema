package com.exasol.adapter.dialects.athena;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exasol.adapter.dialects.JDBCAdapterContext;

class AthenaSqlDialectFactoryTest {
    private AthenaSqlDialectFactory factory;

    @BeforeEach
    void beforeEach() {
        this.factory = new AthenaSqlDialectFactory();
    }

    @Test
    void testGetName() {
        assertThat(this.factory.getSqlDialectName(), equalTo("ATHENA"));
    }

    @Test
    void testGetSqlDialectVersion() {
        assertThat(this.factory.getSqlDialectVersion(), equalTo("UNKNOWN"));
    }

    @Test
    void testGetAdapterProjectShortTag() {
        assertThat(this.factory.getAdapterProjectShortTag(), equalTo("VSATHENA"));
    }

    @Test
    void testCreateDialect() {
        assertThat(this.factory.createSqlDialect(JDBCAdapterContext.builder().build()), instanceOf(AthenaSqlDialect.class));
    }
}
