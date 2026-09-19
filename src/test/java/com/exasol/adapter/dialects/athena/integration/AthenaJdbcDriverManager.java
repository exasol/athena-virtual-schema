package com.exasol.adapter.dialects.athena.integration;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.exasol.drivers.JdbcDriver;

/** Pinned Athena JDBC driver artifact used by the integration tests. */
final class AthenaJdbcDriverManager {
    // https://docs.aws.amazon.com/athena/latest/ug/jdbc-v3-driver.html
    private static final String FILE_NAME = "athena-jdbc-3.8.1-with-dependencies.jar";
    private static final URI DOWNLOAD_URI = URI.create(
            "https://downloads.athena.us-east-1.amazonaws.com/drivers/JDBC/3.8.1/" + FILE_NAME);
    private static final String SHA_256 = "271d6811887c8c7cb6096b44988af1d950bb8f9943ba4d2bfd4e21a4f2172932";
    private static final Path FILE = Path.of("target", FILE_NAME);
    private final Path file;

    private AthenaJdbcDriverManager(final Path file) {
        this.file = file;
    }

    static AthenaJdbcDriverManager download() {
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.isRegularFile(FILE) || !validateChecksum()) {
                doDownload();
            }
            if (!validateChecksum()) {
                throw new IllegalStateException("Downloaded Athena JDBC driver checksum does not match the pinned SHA-256.");
            }
            return new AthenaJdbcDriverManager(FILE);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to download the pinned Athena JDBC driver.", exception);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to download the pinned Athena JDBC driver.", exception);
        }
    }

    private static boolean validateChecksum() {
        return SHA_256.equals(sha256(FILE));
    }

    private static void doDownload() throws IOException, InterruptedException {
        final HttpResponse<InputStream> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(DOWNLOAD_URI).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Athena JDBC driver download returned HTTP " + response.statusCode() + ".");
        }
        try (InputStream body = response.body()) {
            Files.copy(body, FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    String fileName() {
        return FILE_NAME;
    }

    JdbcDriver asExasolDriver() {
        return JdbcDriver.builder("ATHENA_JDBC_DRIVER")
                .enableSecurityManager(false)
                .mainClass("com.amazon.athena.jdbc.AthenaDriver")
                .prefix("jdbc:athena:")
                .sourceFile(this.file)
                .build();
    }

    private static String sha256(final Path file) {
        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
            final StringBuilder result = new StringBuilder();
            for (final byte value : hash) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (final IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to calculate the Athena JDBC driver checksum.", exception);
        }
    }
}
