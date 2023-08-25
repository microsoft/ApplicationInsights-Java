// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
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
  static class Java8Test extends JavaProfilerDisabledTest {}

  @Environment(JAVA_11)
  static class Java11Test extends JavaProfilerDisabledTest {}

  @Environment(JAVA_17)
  static class Java17Test extends JavaProfilerDisabledTest {}

  @Environment(JAVA_20)
  static class JavaLatestTest extends JavaProfilerDisabledTest {}

  @Environment(JAVA_21)
  static class JavaPrereleaseTest extends JavaProfilerDisabledTest {}
}
