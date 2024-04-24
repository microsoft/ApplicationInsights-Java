// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class OtelResourceCustomMetricTest {

  private static final String OTEL_RESOURCE_ATTRIBUTES =
      "cloud.resource_id=/subscriptions/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxdfcdaa1/resourceGroups/fake-aks-cluster-name/providers/Microsoft.ContainerService/managedClusters/aks-vanilla-1,cloud.region=eastus,k8s.cluster.name=aks-vanilla-1,k8s.pod.namespace=default,k8s.node.name=aks-agentpool-19737836-vmss000001,k8s.pod.name=customer-test-app-78d8bf887c-nplmk,k8s.pod.uid=efa8fdda-873c-4a6f-b02a-8d3b00bc34a7,k8s.container.name=test-app-java,cloud.provider=Azure,cloud.platform=azure_aks,k8s.replicaset.name=customer-java-very-big-756899c8b6,k8s.deployment.name=customer-java-very-big,k8s.replicaset.uid=69ef5d52-c770-4ec7-a3a2-2e2e8e885b3d";

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().otelResourceAttributesEnvVar(OTEL_RESOURCE_ATTRIBUTES).build();

  private static final int COUNT = 100;

  @Test
  @TargetUri(value = "/app", callCount = COUNT)
  void testApp() {
    List<Envelope> metricsEnvelops = testing.mockedIngestion.getItemsEnvelopeDataType("MetricData");
    List<Envelope> otelResourceCustomMetrics = new ArrayList<>();
    for (Envelope envelope : metricsEnvelops) {
      MetricData metricData = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      String name = metricData.getMetrics().get(0).getName();
      if ("_OTELRESOURCE_".equals(name)) {
        otelResourceCustomMetrics.add(envelope);
      }
    }

    verify(otelResourceCustomMetrics);
  }

  private static void verify(List<Envelope> metrics) {
    for (Envelope envelope : metrics) {
      validateTags(envelope);
      MetricData md1 = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      validateMetricData(md1);
    }
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "customer-test-app-78d8bf887c-nplmk");
    assertThat(tags).containsEntry("ai.cloud.role", "customer-java-very-big");
  }

  private static void validateMetricData(MetricData metricData) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    Map<String, String> properties = metricData.getProperties();
    assertThat(properties).containsAllEntriesOf(parseOtelResourceAttributes());
  }

  private static Map<String, String> parseOtelResourceAttributes() {
    String[] attributes = OTEL_RESOURCE_ATTRIBUTES.split(",");
    Map<String, String> result = new HashMap<>();
    for (String attribute : attributes) {
      String[] keyValue = attribute.split("=");
      if (keyValue.length == 2) {
        result.put(keyValue[0], keyValue[1]);
      }
    }
    return result;
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends OtelResourceCustomMetricTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends OtelResourceCustomMetricTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OtelResourceCustomMetricTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends OtelResourceCustomMetricTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends OtelResourceCustomMetricTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends OtelResourceCustomMetricTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OtelResourceCustomMetricTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OtelResourceCustomMetricTest {}
}
