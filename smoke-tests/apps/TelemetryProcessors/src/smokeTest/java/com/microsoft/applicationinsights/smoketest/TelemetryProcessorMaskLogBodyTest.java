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
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-mask-log-body.json")
abstract class TelemetryProcessorMaskLogBodyTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/mask-user-id-in-log-body")
  void maskUserIdInLogBody() throws Exception {
    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(1);
    MessageData md1 = logs.get(0);
    assertThat(md1.getMessage())
        .isEqualTo("User account with userId {redactedUserId} failed to login");
    assertThat(md1.getProperties().get("redactedUserId")).isNull();
  }

  @Test
  @TargetUri("/mask-email-in-log-body")
  void masEmailInLogBody() throws Exception {
    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(1);
    MessageData md1 = logs.get(0);
    assertThat(md1.getMessage())
        .isEqualTo(
            "This is my \"email\" : \"{redactedEmail}\" and my \"phone\" : \"{redactedPhone}\"");
    assertThat(md1.getProperties().get("redactedEmail")).isNull();
    assertThat(md1.getProperties().get("redactedPhone")).isNull();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TelemetryProcessorMaskLogBodyTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends TelemetryProcessorMaskLogBodyTest {}
}
