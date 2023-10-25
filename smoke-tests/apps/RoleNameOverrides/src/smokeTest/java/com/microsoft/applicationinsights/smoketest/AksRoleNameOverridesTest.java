// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
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
    String jbossHome = System.getenv("JBOSS_HOME");
    if (jbossHome != null) {
     // jboss/wildfly has a default service.name="jboss-module"
      verifyRoleNameAndInstance("jboss-module", "test-pod-name");
    } else {
      verifyRoleNameAndInstance("test-deployment-name", "test-pod-name");
    }
  }

  private static void verifyRoleNameAndInstance(String roleName, String roleInstance)
      throws Exception {
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
        testing.mockedIngestion.waitForMetricItems("http.client.duration", 1);
    Map<String, String> clientTags = clientMetrics.get(0).getTags();
    assertThat(clientTags.get("ai.cloud.role")).isEqualTo(roleName);
    assertThat(clientTags.get("ai.cloud.roleInstance")).isEqualTo(roleInstance);

    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForMetricItems("http.server.duration", 1);
    Map<String, String> serverTags = serverMetrics.get(0).getTags();
    assertThat(serverTags.get("ai.cloud.role")).isEqualTo(roleName);
    assertThat(serverTags.get("ai.cloud.roleInstance")).isEqualTo(roleInstance);
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

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends AksRoleNameOverridesTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends AksRoleNameOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends AksRoleNameOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends AksRoleNameOverridesTest {}
}
