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
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_18;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights.json")
abstract class SamplingOverrideTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

  @Test
  @TargetUri(value = "/health-check", callCount = 100)
  void testSampling() throws Exception {
    // super super low chance that number of sampled requests is less than 25
    long start = System.nanoTime();
    while (testing.mockedIngestion.getCountForType("RequestData") < 25
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {}
    // wait ten more seconds to before checking that we didn't receive too many
    Thread.sleep(SECONDS.toMillis(10));
    int requestCount = testing.mockedIngestion.getCountForType("RequestData");
    int dependencyCount = testing.mockedIngestion.getCountForType("RemoteDependencyData");
    // super super low chance that number of sampled requests/dependencies
    // is less than 25 or greater than 75
    assertThat(requestCount).isGreaterThanOrEqualTo(25);
    assertThat(dependencyCount).isGreaterThanOrEqualTo(25);
    assertThat(requestCount).isLessThanOrEqualTo(75);
    assertThat(dependencyCount).isLessThanOrEqualTo(75);
  }

  @Test
  @TargetUri("/noisy-jdbc")
  void testNoisyJdbc() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Thread.sleep(10000);
    assertThat(testing.mockedIngestion.getCountForType("RemoteDependencyData")).isZero();

    Envelope rdEnvelope = rdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    assertThat(rdEnvelope.getIKey()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(rdEnvelope.getTags()).containsEntry("ai.cloud.role", "testrolename");
    assertThat(rd.getSuccess()).isTrue();
  }

  @Test
  @TargetUri("/regular-jdbc")
  void testRegularJdbc() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rddEnvelope = rddList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdEnvelope.getIKey()).isEqualTo("87654321-0000-0000-0000-0FEEDDADBEEF");
    assertThat(rdEnvelope.getTags()).containsEntry("ai.cloud.role", "app3");
    assertThat(rd.getSuccess()).isTrue();

    assertThat(rdd.getType()).isEqualTo("SQL");
    assertThat(rdd.getTarget()).isEqualTo("testdb");
    assertThat(rdd.getName()).isEqualTo("SELECT testdb.abc");
    assertThat(rdd.getData()).isEqualTo("select * from abc");
    assertThat(rddEnvelope.getIKey()).isEqualTo("87654321-0000-0000-0000-0FEEDDADBEEF");
    assertThat(rddEnvelope.getTags()).containsEntry("ai.cloud.role", "app3");
    assertThat(rdd.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(rd, rdEnvelope, rddEnvelope, "GET /TelemetryFiltering/*");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SamplingOverrideTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SamplingOverrideTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SamplingOverrideTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SamplingOverrideTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SamplingOverrideTest {}

  @Environment(TOMCAT_8_JAVA_18)
  static class Tomcat8Java18Test extends SamplingOverrideTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends SamplingOverrideTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SamplingOverrideTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SamplingOverrideTest {}
}
