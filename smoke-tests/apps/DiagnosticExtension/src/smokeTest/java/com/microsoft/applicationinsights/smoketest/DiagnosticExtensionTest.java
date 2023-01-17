// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;

import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class DiagnosticExtensionTest {
  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .addAgentExtensionfile(new File("MockExtension/build/libs/extension.jar"))
          .build();

  @Test
  @TargetUri("/")
  void doDelayedDiagnosticExtensionTest() throws Exception {
    String url = testing.getBaseUrl() + "/detectIfExtensionInstalled";
    String response = HttpHelper.get(url, "");

    Assertions.assertTrue(Boolean.parseBoolean(response));
  }

  @Environment(JAVA_17)
  static class Java17Test extends DiagnosticExtensionTest {}
}
