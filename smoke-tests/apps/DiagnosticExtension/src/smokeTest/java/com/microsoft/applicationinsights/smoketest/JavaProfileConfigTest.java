// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;

import com.microsoft.applicationinsights.smoketest.fakeingestion.ProfilerState;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class JavaProfileConfigTest {

  static final SmokeTestExtensionBuilder BASE_BUILDER =
      SmokeTestExtension.builder()
          .setAgentExtensionFile(new File("MockExtension/build/libs/extension.jar"));

  private final SmokeTestExtension testing;
  private final boolean shouldBeEnabled;

  JavaProfileConfigTest(SmokeTestExtension testing, boolean shouldBeEnabled) {
    this.testing = testing;
    this.shouldBeEnabled = shouldBeEnabled;
  }

  @Test
  @TargetUri("/")
  void testIfExtensionIsLoaded() throws Exception {
    String url = testing.getBaseUrl() + "/detectIfExtensionInstalled";
    String response = HttpHelper.get(url, "");

    Assertions.assertEquals(shouldBeEnabled, Boolean.parseBoolean(response));
  }

  @Environment(JAVA_11)
  static class JavaProfilerConfiguredTest extends JavaProfileConfigTest {
    @RegisterExtension
    static final SmokeTestExtension testing =
        BASE_BUILDER.setProfilerEndpoint(ProfilerState.configuredEnabled).build();

    JavaProfilerConfiguredTest() {
      super(testing, true);
    }
  }

  @Environment(JAVA_11)
  static class JavaProfilerUnconfiguredTest extends JavaProfileConfigTest {
    @RegisterExtension
    static final SmokeTestExtension testing =
        BASE_BUILDER.setProfilerEndpoint(ProfilerState.unconfigured).build();

    JavaProfilerUnconfiguredTest() {
      super(testing, false);
    }
  }

  @Environment(JAVA_11)
  static class JavaProfilerDisabledTest extends JavaProfileConfigTest {
    @RegisterExtension
    static final SmokeTestExtension testing =
        BASE_BUILDER.setProfilerEndpoint(ProfilerState.configuredDisabled).build();

    JavaProfilerDisabledTest() {
      super(testing, false);
    }
  }

  @Environment(JAVA_11)
  static class JavaProfilerManualProfileTest extends JavaProfileConfigTest {
    @RegisterExtension
    static final SmokeTestExtension testing =
        BASE_BUILDER.setProfilerEndpoint(ProfilerState.manualprofile).build();

    JavaProfilerManualProfileTest() {
      super(testing, true);
    }
  }
}
