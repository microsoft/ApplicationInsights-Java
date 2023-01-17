// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;

import com.microsoft.applicationinsights.smoketest.annotations.AdditionalFile;
import com.microsoft.applicationinsights.smoketest.annotations.JvmArgs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


abstract class DiagnosticExtensionTest {
  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/")
  void doDelayedDiagnosticExtensionTest() throws Exception {
    String url = testing.getBaseUrl() + "/detectIfExtensionInstalled";
    String response = HttpHelper.get(url, "");

    Assertions.assertTrue(Boolean.parseBoolean(response));
  }

  @Environment(JAVA_17)
  @UseAgent("applicationinsights.json")
  @JvmArgs(args = {"-Dotel.javaagent.extensions=/extension.jar"})
  @AdditionalFile(testFile = "MockExtension/build/libs/extension.jar", targetFile = "/extension.jar")
  static class Java17Test extends DiagnosticExtensionTest {}
}
