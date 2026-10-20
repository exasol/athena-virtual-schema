package com.exasol.adapter.dialects.athena;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AthenaColumnMetadataReaderTest {
    @ParameterizedTest
    @ValueSource(strings = { "timestamp with time zone", "timestamp(6) with timezone", "TIMESTAMP WITH TIME ZONE" })
    void recognizesZonedTimestampTypeNames(final String typeName) {
        assertThat(AthenaColumnMetadataReader.isTimestampWithTimeZone(typeName), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = { "timestamp", "time with time zone", "varchar" })
    void doesNotRecognizeOtherTypeNames(final String typeName) {
        assertThat(AthenaColumnMetadataReader.isTimestampWithTimeZone(typeName), is(false));
    }
}
