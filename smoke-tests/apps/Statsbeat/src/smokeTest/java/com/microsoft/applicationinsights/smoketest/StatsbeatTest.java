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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class StatsbeatTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/index.jsp")
  void testStatsbeat() throws Exception {
    List<Envelope> metrics =
        testing.mockedIngestion.waitForMetricItems("Feature", 2, 70, TimeUnit.SECONDS);

    // sort envelope list to have feature at the beginning and instrumentation at the end
    Envelope[] sortedMetrics = new Envelope[2];
    for (int i = 0; i < metrics.size(); i++) {
      MetricData data = (MetricData) ((Data<?>) metrics.get(i).getData()).getBaseData();
      if ("0".equals(data.getProperties().get("type"))) {
        sortedMetrics[0] = metrics.get(i);
      } else {
        sortedMetrics[1] = metrics.get(i);
      }
    }

    MetricData data = (MetricData) ((Data<?>) sortedMetrics[0].getData()).getBaseData();
    assertCommon(data);
    assertThat(data.getProperties()).containsKey("feature");
    assertThat(data.getProperties()).containsKey("type");
    assertThat(data.getProperties()).containsEntry("type", "0");
    assertThat(data.getProperties()).hasSize(9);

    MetricData instrumentationData =
        (MetricData) ((Data<?>) sortedMetrics[1].getData()).getBaseData();
    assertCommon(instrumentationData);
    assertThat(instrumentationData.getProperties()).containsKey("feature");
    assertThat(instrumentationData.getProperties()).containsKey("type");
    assertThat(instrumentationData.getProperties()).containsEntry("type", "1");
    assertThat(instrumentationData.getProperties()).hasSize(9);

    List<Envelope> attachMetrics =
        testing.mockedIngestion.waitForMetricItems("Attach", 1, 70, TimeUnit.SECONDS);

    MetricData attachData = (MetricData) ((Data<?>) attachMetrics.get(0).getData()).getBaseData();
    assertCommon(attachData);
    assertThat(attachData.getProperties()).containsKey("rpId");
    assertThat(attachData.getProperties()).hasSize(8);

    List<Envelope> requestSuccessCountMetrics =
        testing.mockedIngestion.waitForMetricItems(
            "Request_Success_Count", 1, 70, TimeUnit.SECONDS);

    MetricData requestSuccessCountData =
        (MetricData) ((Data<?>) requestSuccessCountMetrics.get(0).getData()).getBaseData();
    assertCommon(requestSuccessCountData);
    assertThat(requestSuccessCountData.getProperties()).containsKey("endpoint");
    assertThat(requestSuccessCountData.getProperties()).containsKey("host");
    assertThat(requestSuccessCountData.getProperties()).hasSize(9);

    List<Envelope> requestDurationMetrics =
        testing.mockedIngestion.waitForMetricItems("Request_Duration", 1, 70, TimeUnit.SECONDS);

    MetricData requestDurationData =
        (MetricData) ((Data<?>) requestDurationMetrics.get(0).getData()).getBaseData();
    assertCommon(requestDurationData);
    assertThat(requestSuccessCountData.getProperties()).containsKey("endpoint");
    assertThat(requestSuccessCountData.getProperties()).containsKey("host");
    assertThat(requestDurationData.getProperties()).hasSize(9);
  }

  private void assertCommon(MetricData metricData) {
    assertThat(metricData.getProperties()).containsKey("rp");
    assertThat(metricData.getProperties()).containsKey("attach");
    assertThat(metricData.getProperties()).containsKey("cikey");
    assertThat(metricData.getProperties()).containsKey("runtimeVersion");
    assertThat(metricData.getProperties()).containsKey("os");
    assertThat(metricData.getProperties()).containsKey("language");
    assertThat(metricData.getProperties()).containsKey("version");
    // customer defined dimensions do not apply to Statsbeat
    assertThat(metricData.getProperties()).doesNotContainEntry("tag1", "abc");
    assertThat(metricData.getProperties()).doesNotContainEntry("tag2", "def");
    assertThat(metricData.getProperties()).doesNotContainEntry("service.version", "123");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends StatsbeatTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends StatsbeatTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends StatsbeatTest {}
}
