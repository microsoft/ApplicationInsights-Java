// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("aks_applicationinsights.json")
abstract class AksRoleNameOverridesTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .otelResourceAttributesEnvVar(
              "cloud.provider=Azure,cloud.platform=azure_aks,k8s.deployment.name=test-deployment-name,k8s.pod.name=test-pod-name")
          .build();

  @Test
  @TargetUri("/testAks")
  void test() throws Exception {
    verifyRoleNameAndInstance("test-deployment-name", "test-pod-name");
  }

  private static void verifyRoleNameAndInstance(String roleName, String roleInstance)
      throws Exception {

    // verify _OTELRESOURCE_ custom metric per role name
    List<Envelope> otelResourceMetrics =
        testing.mockedIngestion.waitForMetricItems("_OTELRESOURCE_", roleName, 1, true);
    verifyOtelResourceAttributeCustomMetric(otelResourceMetrics);

    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    assertThat(rdEnvelope.getTags()).containsEntry("ai.cloud.role", roleName);
    assertThat(rdEnvelope.getTags()).containsEntry("ai.cloud.roleInstance", roleInstance);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getSuccess()).isTrue();

    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    assertThat(rddList.get(0).getTags()).containsEntry("ai.cloud.role", roleName);
    assertThat(rddList.get(0).getTags()).containsEntry("ai.cloud.roleInstance", roleInstance);
    List<Envelope> mdList =
        testing.mockedIngestion.waitForItemsInOperation("MessageData", 1, operationId);
    assertThat(mdList.get(0).getTags()).containsEntry("ai.cloud.role", roleName);
    assertThat(mdList.get(0).getTags()).containsEntry("ai.cloud.roleInstance", roleInstance);

    List<Envelope> clientMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("dependencies/duration", 1);
    Map<String, String> clientTags = clientMetrics.get(0).getTags();
    assertThat(clientTags.get("ai.cloud.role")).isEqualTo(roleName);
    assertThat(clientTags.get("ai.cloud.roleInstance")).isEqualTo(roleInstance);

    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("requests/duration", 1);
    Map<String, String> serverTags = serverMetrics.get(0).getTags();
    assertThat(serverTags.get("ai.cloud.role")).isEqualTo(roleName);
    assertThat(serverTags.get("ai.cloud.roleInstance")).isEqualTo(roleInstance);
  }

  private static void verifyOtelResourceAttributeCustomMetric(List<Envelope> otelResourceMetrics) {
    Map<String, String> tags = otelResourceMetrics.get(0).getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags.get("ai.cloud.roleInstance")).isEqualTo("test-pod-name");
    assertThat(tags.get("ai.cloud.role")).isEqualTo("test-deployment-name");

    MetricData otelResourceMetricData =
        (MetricData) ((Data<?>) otelResourceMetrics.get(0).getData()).getBaseData();
    Map<String, String> properties = otelResourceMetricData.getProperties();
    assertThat(properties.get("cloud.provider")).isEqualTo("Azure");
    assertThat(properties.get("cloud.platform")).isEqualTo("azure_aks");
    assertThat(properties.get("telemetry.sdk.language")).isEqualTo("java");
    assertThat(properties.get("service.name")).isEqualTo("test-deployment-name");
    assertThat(properties.get("service.instance.id")).isEqualTo("test-pod-name");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends AksRoleNameOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends AksRoleNameOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends AksRoleNameOverridesTest {}
}
