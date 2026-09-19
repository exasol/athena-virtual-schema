package com.exasol.athena.ciisolation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Loads the mandatory AWS resource tags for the integration-test fixture. */
final class FixtureTags {
    private static final Path CONFIG_FILE = Path.of("..", "test_config.properties").toAbsolutePath();

    private FixtureTags() {
    }

    static Map<String, String> load() {
        final Properties properties = getConfigFile();
        return Map.of(
                "exa:Owner", requireValue(properties, "tag.exaOwner"),
                "exa:Environment", "development",
                "exa:Department", requireValue(properties, "tag.exaDepartment"),
                "exa:CostCenter", requireValue(properties, "tag.exaCostCenter"),
                "exa:Project", requireValue(properties, "tag.exaProject"),
                "exa:Workload", "Integration test for Athena Virtual Schema");
    }

    private static Properties getConfigFile() {
        final Properties properties = new Properties();
        try (var input = Files.newInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to read test configuration from '" + CONFIG_FILE + "'.", exception);
        }
        return properties;
    }

    private static String requireValue(final Properties properties, final String propertyName) {
        final String value = properties.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property '" + propertyName + "' in " + CONFIG_FILE);
        }
        return value;
    }
}
