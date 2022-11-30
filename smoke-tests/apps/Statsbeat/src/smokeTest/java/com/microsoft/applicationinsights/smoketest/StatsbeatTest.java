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

    MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
    assertCommon(data);
    assertThat(data.getProperties()).containsKey("feature");
    assertThat(data.getProperties()).containsKey("type");
    assertThat(data.getProperties()).containsEntry("type", "0");
    assertThat(data.getProperties()).hasSize(9);

    MetricData instrumentationData =
        (MetricData) ((Data<?>) metrics.get(1).getData()).getBaseData();
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
            "Request Success Count", 1, 70, TimeUnit.SECONDS);

    MetricData requestSuccessCountData =
        (MetricData) ((Data<?>) requestSuccessCountMetrics.get(0).getData()).getBaseData();
    assertCommon(requestSuccessCountData);
    assertThat(requestSuccessCountData.getProperties()).containsKey("endpoint");
    assertThat(requestSuccessCountData.getProperties()).containsKey("host");
    assertThat(requestSuccessCountData.getProperties()).hasSize(9);

    List<Envelope> requestDurationMetrics =
        testing.mockedIngestion.waitForMetricItems("Request Duration", 1, 70, TimeUnit.SECONDS);

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

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends StatsbeatTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends StatsbeatTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends StatsbeatTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends StatsbeatTest {}
}
