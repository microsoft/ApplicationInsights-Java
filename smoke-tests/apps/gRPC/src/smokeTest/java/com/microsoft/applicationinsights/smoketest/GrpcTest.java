// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class GrpcTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/simple")
  void doSimpleTest() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("GET /simple")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasTag("ai.operation.name", "GET /simple")
                            .hasNoParent())
                .hasDependencySatisying(
                    dependency ->
                        dependency
                            .hasName("example.Greeter/SayHello")
                            .hasTarget("localhost:10203")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasTag("ai.operation.name", "GET /simple")
                            .hasParent(trace.getRequestId(0)))
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("example.Greeter/SayHello")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasTag("ai.operation.name", "example.Greeter/SayHello")
                            .hasParent(trace.getDependencyId(0)))
                // auto-collected grpc events are suppressed by exporter because they are noisy
                .hasMessageCount(0));

    testing.waitAndAssertMetric(
        "rpc.client.duration",
        metric ->
            metric
                .hasValueGreaterThanZero()
                .hasCount(1)
                .hasMinGreaterThanZero()
                .hasMaxGreaterThanZero()
                .containsTagKey("ai.internal.sdkVersion")
                .containsTags(
                    entry("ai.cloud.roleInstance", "testroleinstance"),
                    entry("ai.cloud.role", "testrolename"))
                .containsPropertiesExactly(
                    entry("_MS.MetricId", "dependencies/duration"),
                    entry("dependency/target", "localhost:10203"),
                    entry("Dependency.Success", "True"),
                    entry("Dependency.Type", "grpc"),
                    entry("operation/synthetic", "False"),
                    entry("cloud/roleInstance", "testroleinstance"),
                    entry("cloud/roleName", "testrolename"),
                    entry("_MS.IsAutocollected", "True"),
                    entry("_MS.SentToAMW", "False")));

    testing.waitAndAssertMetric(
        "rpc.server.duration",
        metric ->
            metric
                .hasValueGreaterThanZero()
                .hasCount(1)
                .hasMinGreaterThanZero()
                .hasMaxGreaterThanZero()
                .containsTagKey("ai.internal.sdkVersion")
                .containsTags(
                    entry("ai.cloud.roleInstance", "testroleinstance"),
                    entry("ai.cloud.role", "testrolename"))
                .containsPropertiesExactly(
                    entry("_MS.MetricId", "requests/duration"),
                    entry("Request.Success", "True"),
                    entry("operation/synthetic", "False"),
                    entry("cloud/roleInstance", "testroleinstance"),
                    entry("cloud/roleName", "testrolename"),
                    entry("_MS.IsAutocollected", "True"),
                    entry("_MS.SentToAMW", "False")));
  }

  @Test
  @TargetUri("/conversation")
  void doConversationTest() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("GET /conversation")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasTag("ai.operation.name", "GET /conversation")
                            .hasNoParent())
                .hasDependencySatisying(
                    dependency ->
                        dependency
                            .hasName("example.Greeter/Conversation")
                            .hasTarget("localhost:10203")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasTag("ai.operation.name", "GET /conversation")
                            .hasParent(trace.getRequestId(0)))
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("example.Greeter/Conversation")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasTag("ai.operation.name", "example.Greeter/Conversation")
                            .hasParent(trace.getDependencyId(0)))
                // auto-collected grpc events are suppressed by exporter because they are noisy
                .hasMessageCount(0));

    testing.waitAndAssertMetric(
        "rpc.client.duration",
        metric ->
            metric
                .hasValueGreaterThanZero()
                .hasCount(1)
                .hasMinGreaterThanZero()
                .hasMaxGreaterThanZero()
                .containsTagKey("ai.internal.sdkVersion")
                .containsTags(
                    entry("ai.cloud.roleInstance", "testroleinstance"),
                    entry("ai.cloud.role", "testrolename"))
                .containsPropertiesExactly(
                    entry("_MS.MetricId", "dependencies/duration"),
                    entry("dependency/target", "localhost:10203"),
                    entry("Dependency.Success", "True"),
                    entry("Dependency.Type", "grpc"),
                    entry("operation/synthetic", "False"),
                    entry("cloud/roleInstance", "testroleinstance"),
                    entry("cloud/roleName", "testrolename"),
                    entry("_MS.IsAutocollected", "True"),
                    entry("_MS.SentToAMW", "False")));

    testing.waitAndAssertMetric(
        "rpc.server.duration",
        metric ->
            metric
                .hasValueGreaterThanZero()
                .hasCount(1)
                .hasMinGreaterThanZero()
                .hasMaxGreaterThanZero()
                .containsTagKey("ai.internal.sdkVersion")
                .containsTags(
                    entry("ai.cloud.roleInstance", "testroleinstance"),
                    entry("ai.cloud.role", "testrolename"))
                .containsPropertiesExactly(
                    entry("_MS.MetricId", "requests/duration"),
                    entry("Request.Success", "True"),
                    entry("operation/synthetic", "False"),
                    entry("cloud/roleInstance", "testroleinstance"),
                    entry("cloud/roleName", "testrolename"),
                    entry("_MS.IsAutocollected", "True"),
                    entry("_MS.SentToAMW", "False")));
  }

  @Environment(JAVA_8)
  static class Java8Test extends GrpcTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_11)
  static class Java11Test extends GrpcTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_17)
  static class Java17Test extends GrpcTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_21)
  static class Java21Test extends GrpcTest {}

  @Environment(JAVA_21_OPENJ9)
  static class Java21OpenJ9Test extends GrpcTest {}
}
