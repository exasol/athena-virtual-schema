package com.exasol.athena.ciisolation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Loads the mandatory AWS resource tags for the integration-test fixture. */
final class FixtureTags {
    private FixtureTags() {
    }

    static Map<String, String> load() {
        final Properties properties = getConfigFile();
        return Map.of(
                "exa:Owner", required(properties, "tag.exaOwner"),
                "exa:Environment", "development",
                "exa:Department", required(properties, "tag.exaDepartment"),
                "exa:CostCenter", required(properties, "tag.exaCostCenter"),
                "exa:Project", required(properties, "tag.exaProject"),
                "exa:Workload", "Integration test for Athena Virtual Schema");
    }

    private static Properties getConfigFile() {
        final Properties properties = new Properties();
        final Path configFile = findConfigFile();
        try (var input = Files.newInputStream(configFile)) {
            properties.load(input);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to read test configuration from '" + configFile + "'.", exception);
        }
        return properties;
    }

    private static Path findConfigFile() {
        final Path rootConfig = Path.of("test_config.properties");
        if (Files.isRegularFile(rootConfig)) {
            return rootConfig;
        }
        final Path moduleConfig = Path.of("..", "test_config.properties");
        if (Files.isRegularFile(moduleConfig)) {
            return moduleConfig;
        }
        throw new IllegalStateException("Could not find test_config.properties in the repository root.");
    }

    private static String required(final Properties properties, final String propertyName) {
        final String value = properties.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property '" + propertyName + "' in test_config.properties.");
        }
        return value;
    }
}
