package com.exasol.athena.ciisolation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.athena.CfnWorkGroup;
import software.amazon.awscdk.services.athena.CfnWorkGroupProps;
import software.amazon.awscdk.services.glue.CfnDatabase;
import software.amazon.awscdk.services.glue.CfnDatabaseProps;
import software.amazon.awscdk.services.glue.CfnTable;
import software.amazon.awscdk.services.glue.CfnTableProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

/** Defines the shared, read-only data fixture used by local Athena integration tests. */
public class AthenaFixtureStack extends Stack {
    private static final String DATABASE_NAME = "athena_virtual_schema_it";
    private static final String TABLE_NAME = "pushdown_fixture";
    private static final String WORKGROUP_NAME = "athena-virtual-schema-it";

    public AthenaFixtureStack(final Construct scope, final String id, final Map<String, String> tags) {
        this(scope, id, null, tags);
    }

    private AthenaFixtureStack(final Construct scope, final String id, final StackProps props, final Map<String, String> tags) {
        super(scope, id, props);
        tags.forEach((key, value) -> Tags.of(this).add(key, value));
        final Bucket fixtureBucket = Bucket.Builder.create(this, "FixtureBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(false)
                .autoDeleteObjects(true)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        BucketDeployment.Builder.create(this, "FixtureData")
                .destinationBucket(fixtureBucket)
                .destinationKeyPrefix("fixture")
                .sources(List.of(Source.asset(fixtureAssetPath())))
                .build();
        final CfnDatabase database = new CfnDatabase(this, "FixtureDatabase",
                CfnDatabaseProps.builder().catalogId(this.getAccount()).databaseInput(CfnDatabase.DatabaseInputProperty.builder()
                        .name(DATABASE_NAME).build()).build());
        new CfnTable(this, "FixtureTable", CfnTableProps.builder().catalogId(this.getAccount()).databaseName(DATABASE_NAME)
                .tableInput(CfnTable.TableInputProperty.builder().name(TABLE_NAME).tableType("EXTERNAL_TABLE")
                        .parameters(Map.of("classification", "csv", "skip.header.line.count", "1"))
                        .storageDescriptor(CfnTable.StorageDescriptorProperty.builder()
                                .columns(List.of(column("id", "bigint"), column("name", "string"),
                                        column("amount", "decimal(10,2)"), column("active", "boolean"),
                                        column("created_on", "date"), column("created_at", "timestamp")))
                                .location(fixtureBucket.s3UrlForObject("fixture/")).inputFormat("org.apache.hadoop.mapred.TextInputFormat")
                                .outputFormat("org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat")
                                .serdeInfo(CfnTable.SerdeInfoProperty.builder()
                                        .serializationLibrary("org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe")
                                        .parameters(Map.of("field.delim", ",", "serialization.format", ",")).build())
                                .build())
                        .build())
                .build()).addResourceDependency(database);
        final String resultLocation = fixtureBucket.s3UrlForObject("results/");
        new CfnWorkGroup(this, "WorkGroup", CfnWorkGroupProps.builder().name(WORKGROUP_NAME)
                .workGroupConfiguration(CfnWorkGroup.WorkGroupConfigurationProperty.builder().enforceWorkGroupConfiguration(true)
                        .resultConfiguration(CfnWorkGroup.ResultConfigurationProperty.builder().outputLocation(resultLocation).build())
                        .build())
                .build());
        output("Region", this.getRegion());
        output("Workgroup", WORKGROUP_NAME);
        output("Database", DATABASE_NAME);
        output("Table", TABLE_NAME);
        output("OutputLocation", resultLocation);
    }

    private static CfnTable.ColumnProperty column(final String name, final String type) {
        return CfnTable.ColumnProperty.builder().name(name).type(type).build();
    }

    private static String fixtureAssetPath() {
        final Path modulePath = Path.of("src", "main", "resources", "fixture");
        if (Files.isDirectory(modulePath)) {
            return modulePath.toString();
        }
        return Path.of("ci-isolation").resolve(modulePath).toString();
    }

    private void output(final String name, final String value) {
        new CfnOutput(this, name, CfnOutputProps.builder().value(value).exportName(this.getStackName() + '-' + name).build());
    }
}
