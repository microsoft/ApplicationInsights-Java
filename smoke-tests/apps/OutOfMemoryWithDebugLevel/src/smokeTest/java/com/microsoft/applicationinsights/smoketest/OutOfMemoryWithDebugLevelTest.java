// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class OutOfMemoryWithDebugLevelTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .setSelfDiagnosticsLevel(
              "debug") // intentionally running with selfDiagnosticLevel "debug"
          .build();

  private static final int COUNT = 100;

  @Test
  @TargetUri(value = "/test", callCount = COUNT)
  void test() throws Exception {
    long start = System.nanoTime();
    while (testing.mockedIngestion.getCountForType("RequestData") < COUNT
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {}
    // wait ten more seconds before checking that we didn't receive too many
    Thread.sleep(SECONDS.toMillis(10));

    List<MessageData> messages = testing.mockedIngestion.getMessageDataInRequest(COUNT);

    assertThat(messages).hasSize(COUNT);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OutOfMemoryWithDebugLevelTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OutOfMemoryWithDebugLevelTest {}
}
