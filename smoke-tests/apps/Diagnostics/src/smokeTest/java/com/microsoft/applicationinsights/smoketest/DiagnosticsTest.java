// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;

import com.microsoft.applicationinsights.smoketest.fakeingestion.ProfilerState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class DiagnosticsTest {

  static final SmokeTestExtensionBuilder BASE_BUILDER = SmokeTestExtension.builder();

  private final SmokeTestExtension testing;

  DiagnosticsTest(SmokeTestExtension testing) {
    this.testing = testing;
  }

  @Test
  @TargetUri("/")
  void getJfr() throws Exception {
    String url = testing.getBaseUrl() + "/jfrFileHasDiagnostics";
    String response = HttpHelper.get(url, "");
    Assertions.assertTrue(Boolean.parseBoolean(response));
  }

  @Environment(JAVA_11)
  static class Java11Test extends DiagnosticsTest {

    @RegisterExtension
    static final SmokeTestExtension testing =
        BASE_BUILDER.setProfilerEndpoint(ProfilerState.manualprofile).build();

    Java11Test() {
      super(testing);
    }
  }
}
