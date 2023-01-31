// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-disabled.json")
abstract class JavaProfilerDisabledTest {
  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.builder().build();

  @Test
  @TargetUri("/")
  void checkJavaProfilerDisabled() throws Exception {
    JavaProfilerEnabledTest.runTest(false, testing);
  }

  @Environment(JAVA_8)
  static class Java8DisabledTest extends JavaProfilerDisabledTest {}

  @Environment(JAVA_11)
  static class Java11DisabledTest extends JavaProfilerDisabledTest {}

  @Environment(JAVA_17)
  static class Java17DisabledTest extends JavaProfilerDisabledTest {}

  @Environment(JAVA_19)
  static class Java19DisabledTest extends JavaProfilerDisabledTest {}

  @Environment(JAVA_20)
  static class Java20DisabledTest extends JavaProfilerDisabledTest {}
}
