/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@UseAgent
abstract class StatsbeatSmokeTest {

  @Test
  @TargetUri(value = "/index.jsp")
  void testStatsbeat() throws Exception {
    List<Envelope> metrics =
        testing.mockedIngestion.waitForItems(
            getMetricPredicate("Feature"), 2, 70, TimeUnit.SECONDS);

    MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
    assertCommon(data);
    assertThat(data.getProperties().get("feature")).isNotNull();
    assertThat(data.getProperties().get("type")).isNotNull();
    assertThat(data.getProperties().get("type")).isEqualTo("0");
    assertThat(data.getProperties()).hasSize(9);

    MetricData instrumentationData =
        (MetricData) ((Data<?>) metrics.get(1).getData()).getBaseData();
    assertCommon(instrumentationData);
    assertThat(instrumentationData.getProperties().get("feature")).isNotNull();
    assertThat(instrumentationData.getProperties().get("type")).isNotNull();
    assertThat(instrumentationData.getProperties().get("type")).isEqualTo("1");
    assertThat(instrumentationData.getProperties()).hasSize(9);

    List<Envelope> attachMetrics =
        testing.mockedIngestion.waitForItems(getMetricPredicate("Attach"), 1, 70, TimeUnit.SECONDS);

    MetricData attachData = (MetricData) ((Data<?>) attachMetrics.get(0).getData()).getBaseData();
    assertCommon(attachData);
    assertThat(attachData.getProperties().get("rpId")).isNotNull();
    assertThat(attachData.getProperties()).hasSize(8);

    List<Envelope> requestSuccessCountMetrics =
        testing.mockedIngestion.waitForItems(
            getMetricPredicate("Request Success Count"), 1, 70, TimeUnit.SECONDS);

    MetricData requestSuccessCountData =
        (MetricData) ((Data<?>) requestSuccessCountMetrics.get(0).getData()).getBaseData();
    assertCommon(requestSuccessCountData);
    assertThat(requestSuccessCountData.getProperties().get("endpoint")).isNotNull();
    assertThat(requestSuccessCountData.getProperties().get("host")).isNotNull();
    assertThat(requestSuccessCountData.getProperties()).hasSize(9);

    List<Envelope> requestDurationMetrics =
        testing.mockedIngestion.waitForItems(
            getMetricPredicate("Request Duration"), 1, 70, TimeUnit.SECONDS);

    MetricData requestDurationData =
        (MetricData) ((Data<?>) requestDurationMetrics.get(0).getData()).getBaseData();
    assertCommon(requestDurationData);
    assertThat(requestSuccessCountData.getProperties().get("endpoint")).isNotNull();
    assertThat(requestSuccessCountData.getProperties().get("host")).isNotNull();
    assertThat(requestDurationData.getProperties()).hasSize(9);
  }

  private void assertCommon(MetricData metricData) {
    assertThat(metricData.getProperties().get("rp")).isNotNull();
    assertThat(metricData.getProperties().get("attach")).isNotNull();
    assertThat(metricData.getProperties().get("cikey")).isNotNull();
    assertThat(metricData.getProperties().get("runtimeVersion")).isNotNull();
    assertThat(metricData.getProperties().get("os")).isNotNull();
    assertThat(metricData.getProperties().get("language")).isNotNull();
    assertThat(metricData.getProperties().get("version")).isNotNull();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends StatsbeatSmokeTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends StatsbeatSmokeTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends StatsbeatSmokeTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends StatsbeatSmokeTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends StatsbeatSmokeTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends StatsbeatSmokeTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends StatsbeatSmokeTest {}
}
