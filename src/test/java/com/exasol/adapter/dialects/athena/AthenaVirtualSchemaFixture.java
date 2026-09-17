package com.exasol.adapter.dialects.athena;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Statement;

import com.exasol.dbbuilder.dialects.exasol.VirtualSchema;

/** Coordinates independent AWS and Exasol fixtures for full virtual-schema integration tests. */
final class AthenaVirtualSchemaFixture implements AutoCloseable {
    // https://docs.aws.amazon.com/athena/latest/ug/jdbc-v3-driver.html
    private static final URI DRIVER_URI = URI.create(
            "https://downloads.athena.us-east-1.amazonaws.com/drivers/JDBC/3.8.1/athena-jdbc-3.8.1-with-dependencies.jar");
    private static final String DRIVER_SHA_256 = "271d6811887c8c7cb6096b44988af1d950bb8f9943ba4d2bfd4e21a4f2172932";
    private static final Path DRIVER_PATH = Path.of("target", "athena-jdbc-3.8.1-with-dependencies.jar");
    private final AthenaFixture athena;
    private final ExasolContainerFixture exasol;

    private AthenaVirtualSchemaFixture(final AthenaFixture athena, final ExasolContainerFixture exasol) {
        this.athena = athena;
        this.exasol = exasol;
    }

    static AthenaVirtualSchemaFixture create() {
        final AthenaFixture athena = AthenaFixture.create();
        try {
            return new AthenaVirtualSchemaFixture(athena, new ExasolContainerFixture(downloadDriver()));
        } catch (final RuntimeException exception) {
            athena.close();
            throw exception;
        }
    }

    VirtualSchema createVirtualSchema() {
        return this.exasol.createVirtualSchema(this.athena);
    }

    Statement statement() {
        return this.exasol.statement();
    }

    String table() {
        return this.athena.table();
    }

    boolean hasExecutedQueryContaining(final String fragment) {
        return this.athena.hasExecutedQueryContaining(fragment);
    }

    private static Path downloadDriver() {
        try {
            Files.createDirectories(DRIVER_PATH.getParent());
            if (!Files.isRegularFile(DRIVER_PATH) || !DRIVER_SHA_256.equals(sha256(DRIVER_PATH))) {
                final HttpRequest request = HttpRequest.newBuilder(DRIVER_URI).GET().build();
                final HttpResponse<InputStream> response = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Athena JDBC driver download returned HTTP " + response.statusCode() + ".");
                }
                try (InputStream body = response.body()) {
                    Files.copy(body, DRIVER_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            if (!DRIVER_SHA_256.equals(sha256(DRIVER_PATH))) {
                throw new IllegalStateException("Downloaded Athena JDBC driver checksum does not match the pinned SHA-256.");
            }
            return DRIVER_PATH;
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to download the pinned Athena JDBC driver.", exception);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to download the pinned Athena JDBC driver.", exception);
        }
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

    @Override
    public void close() {
        try {
            this.exasol.close();
        } finally {
            this.athena.close();
        }
    }
}
