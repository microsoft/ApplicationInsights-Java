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

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8_OPENJ9;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class SpringBootAutoTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

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
    testing.mockedIngestion.waitForItem(
        SmokeTestExtension.getMetricPredicate("counter"), 10, TimeUnit.SECONDS);
  }

  @Environment(JAVA_8)
  static class Java8Test extends SpringBootAutoTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends SpringBootAutoTest {}

  @Environment(JAVA_11)
  static class Java11Test extends SpringBootAutoTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends SpringBootAutoTest {}

  @Environment(JAVA_17)
  static class Java17Test extends SpringBootAutoTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends SpringBootAutoTest {}
}
