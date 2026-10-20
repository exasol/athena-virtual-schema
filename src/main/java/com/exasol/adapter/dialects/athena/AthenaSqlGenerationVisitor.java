package com.exasol.adapter.dialects.athena;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.dialects.rewriting.SqlGenerationVisitor;
import com.exasol.adapter.metadata.DataType.ExaDataType;
import com.exasol.adapter.sql.SqlColumn;

/** Generates Athena SQL while working around JDBC result types unsupported by Exasol. */
final class AthenaSqlGenerationVisitor extends SqlGenerationVisitor {
    AthenaSqlGenerationVisitor(final SqlDialect dialect, final SqlGenerationContext context) {
        super(dialect, context);
    }

    @Override
    public String visit(final SqlColumn column) throws AdapterException {
        final String projection = super.visit(column);
        if (isDirectlyInSelectList(column) && isTimestamp(column)) {
            return "CAST(" + projection + " AS timestamp)";
        }
        return projection;
    }

    private boolean isTimestamp(final SqlColumn column) {
        return column.getMetadata().getType().getExaDataType() == ExaDataType.TIMESTAMP;
    }
}
