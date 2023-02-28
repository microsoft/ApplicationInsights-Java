// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;

import com.microsoft.applicationinsights.smoketest.fakeingestion.ProfilerState;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class SnippetInjectionTest {
  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().setSelfDiagnosticsLevel("trace").build();

  @Test
  @TargetUri("/hello")
  void normalSnippetInjectionTest() throws Exception {
    String url = testing.getBaseUrl() + "/hello";
    String response = HttpHelper.get(url, "");

    System.out.println(response);

    Assertions.assertTrue(Boolean.parseBoolean(response));
  }

  @Environment(JAVA_8)
  static class Java8Test extends SnippetInjectionTest {}

  @Environment(JAVA_11)
  static class Java11Test extends SnippetInjectionTest {}

  @Environment(JAVA_17)
  static class Java17Test extends SnippetInjectionTest {}

  @Environment(JAVA_19)
  static class Java18Test extends SnippetInjectionTest {}

  @Environment(JAVA_20)
  static class Java19Test extends SnippetInjectionTest {}
}
