package com.exasol.adapter.dialects.athena;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.adapternotes.ColumnAdapterNotes;
import com.exasol.adapter.adapternotes.ColumnAdapterNotesJsonConverter;
import com.exasol.adapter.dialects.JDBCAdapterContext;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.sql.SqlColumn;
import com.exasol.adapter.sql.SqlSelectList;

class AthenaSqlGenerationVisitorTest {
    @Test
    void castsDirectProjectionOfTimestamp() throws Exception {
        final SqlColumn column = column("timestamp(6) with time zone");
        column.setParent(SqlSelectList.createRegularSelectList(List.of(column)));

        assertThat(visitor().visit(column), equalTo("CAST(\"zoned_timestamp\" AS timestamp)"));
    }

    private static AthenaSqlGenerationVisitor visitor() {
        return new AthenaSqlGenerationVisitor(new AthenaSqlDialect(JDBCAdapterContext.builder().build()),
                new SqlGenerationContext(null, null, false));
    }

    private static SqlColumn column(final String typeName) {
        final String adapterNotes = ColumnAdapterNotesJsonConverter.getInstance().convertToJson(ColumnAdapterNotes.builder()
                .jdbcDataType(Types.TIMESTAMP).typeName(typeName).build());
        return new SqlColumn(0, ColumnMetadata.builder().name("zoned_timestamp").adapterNotes(adapterNotes)
                .type(DataType.createTimestamp(false, 3)).build());
    }
}
