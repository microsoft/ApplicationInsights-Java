// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_18_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class SystemExitTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/delayedSystemExit")
  void doDelayedSystemExitTest() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 1);
    testing.mockedIngestion.waitForItems("RemoteDependencyData", 1);
    testing.mockedIngestion.waitForItem(
        input -> {
          if (!"MessageData".equals(input.getData().getBaseType())) {
            return false;
          }
          MessageData data = (MessageData) ((Data<?>) input.getData()).getBaseData();
          return data.getMessage().equals("this is an error right before shutdown");
        },
        10,
        TimeUnit.SECONDS);
    testing.mockedIngestion.waitForMetricItems("counter", 1);
  }

  @Environment(JAVA_8)
  static class Java8Test extends SystemExitTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends SystemExitTest {}

  @Environment(JAVA_11)
  static class Java11Test extends SystemExitTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends SystemExitTest {}

  @Environment(JAVA_17)
  static class Java17Test extends SystemExitTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends SystemExitTest {}

  @Environment(JAVA_19)
  static class Java18Test extends SystemExitTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends SystemExitTest {}

  @Environment(JAVA_20)
  static class Java19Test extends SystemExitTest {}
}
