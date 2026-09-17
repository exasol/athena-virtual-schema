package com.exasol.athena.ciisolation;

import software.amazon.awscdk.App;

/** Entry point for the manually operated Athena integration-test fixture stack. */
public final class AthenaFixtureApp {
    private AthenaFixtureApp() {
    }

    public static void main(final String[] args) {
        final App app = new App();
        new AthenaFixtureStack(app, "AthenaVirtualSchemaFixture");
        app.synth();
    }
}
