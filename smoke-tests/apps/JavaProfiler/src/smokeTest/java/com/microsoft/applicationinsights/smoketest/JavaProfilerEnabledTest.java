// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-enabled.json")
abstract class JavaProfilerEnabledTest {
  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.builder().build();

  @Test
  @TargetUri("/")
  void checkJavaProfilerEnabled() throws Exception {
    runTest(true, testing);
  }

  public static void runTest(boolean expectEnabled, SmokeTestExtension testing)
      throws InterruptedException, TimeoutException {
    List<Envelope> metrics =
        testing.mockedIngestion.waitForMetricItems("Feature", 5, 70, TimeUnit.SECONDS);

    metrics.stream()
        .map(it -> (MetricData) ((Data<?>) it.getData()).getBaseData())
        .filter(it -> it.getProperties().get("type").equals("0"))
        .forEach(
            it -> {
              // 35 is flag for Java profiler
              long bits = Long.parseLong(it.getProperties().get("feature")) & (0x1L << 35);

              if (expectEnabled) {
                Assertions.assertNotEquals(0, bits);
              } else {
                Assertions.assertEquals(0, bits);
              }
            });
  }

  @Environment(JAVA_8)
  static class Java8EnabledTest extends JavaProfilerEnabledTest {}

  @Environment(JAVA_11)
  static class Java11EnabledTest extends JavaProfilerEnabledTest {}

  @Environment(JAVA_17)
  static class Java17EnabledTest extends JavaProfilerEnabledTest {}

  @Environment(JAVA_19)
  static class Java18EnabledTest extends JavaProfilerEnabledTest {}

  @Environment(JAVA_20)
  static class Java19EnabledTest extends JavaProfilerEnabledTest {}
}
