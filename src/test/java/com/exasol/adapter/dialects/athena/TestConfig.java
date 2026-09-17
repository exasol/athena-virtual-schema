package com.exasol.adapter.dialects.athena;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.logging.Logger;

import software.amazon.awssdk.auth.credentials.*;

/** Loads local configuration required for real-AWS integration tests. */
final class TestConfig {
    static final String FILE_NAME = "test_config.properties";
    private static final Logger LOGGER = Logger.getLogger(TestConfig.class.getName());
    private static final TestConfig CONFIG = new Reader().readTestConfig();
    private final String awsProfile;
    private final String awsRegion;
    private final String cloudFormationStack;

    private TestConfig(final Properties properties) {
        this.awsProfile = properties.getProperty("awsProfile");
        this.awsRegion = required(properties, "awsRegion");
        this.cloudFormationStack = required(properties, "cloudFormationStack");
    }

    static TestConfig instance() {
        return CONFIG;
    }

    AwsCredentialsProvider getAwsCredentialsProvider() {
        if (this.awsProfile != null && !this.awsProfile.isBlank()) {
            return ProfileCredentialsProvider.create(this.awsProfile);
        }
        return DefaultCredentialsProvider.builder().build();
    }

    String getAwsRegion() {
        return this.awsRegion;
    }

    String getCloudFormationStack() {
        return this.cloudFormationStack;
    }

    private static String required(final Properties properties, final String name) {
        final String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Test configuration property '" + name + "' is required.");
        }
        return value;
    }

    private static final class Reader {
        private TestConfig readTestConfig() {
            final Path path = Path.of(FILE_NAME).toAbsolutePath();
            LOGGER.finer(() -> "Reading test configuration from " + path);
            final Properties properties = new Properties();
            try (InputStream stream = Files.newInputStream(path)) {
                properties.load(stream);
                return new TestConfig(properties);
            } catch (final NoSuchFileException exception) {
                throw new IllegalArgumentException("Could not find " + path + ". Copy " + FILE_NAME
                        + ".example and configure it before running integration tests.", exception);
            } catch (final IOException exception) {
                throw new UncheckedIOException("Failed to load " + path, exception);
            }
        }
    }
}
