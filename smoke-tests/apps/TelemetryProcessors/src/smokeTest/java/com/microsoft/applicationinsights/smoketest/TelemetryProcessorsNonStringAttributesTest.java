// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-non-string-span-attributes.json")
abstract class TelemetryProcessorsNonStringAttributesTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test-non-string-strict-span-attributes")
  void testNonStringStrictSpanAttributes() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);
    Map<String, String> properties = telemetry.rd.getProperties();

    assertThat(properties.get("myLongAttributeKey")).isEqualTo("1234");
    assertThat(properties.get("myBooleanAttributeKey")).isEqualTo("true");
    assertThat(properties.get("myDoubleArrayAttributeKey")).contains("1.0", "2.0", "3.0", "4.0");
    assertThat(properties.get("myNewAttributeKeyStrict")).isEqualTo("myNewAttributeValueStrict");
  }

  @Test
  @TargetUri("/test-non-string-regex-span-attributes")
  void testNonStringRegexSpanAttributes() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);
    Map<String, String> properties = telemetry.rd.getProperties();

    assertThat(properties.get("myLongRegexAttributeKey")).isEqualTo("428");
    assertThat(properties.get("myNewAttributeKeyRegex")).isEqualTo("myNewAttributeValueRegex");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends TelemetryProcessorsNonStringAttributesTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends TelemetryProcessorsNonStringAttributesTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends TelemetryProcessorsNonStringAttributesTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends TelemetryProcessorsNonStringAttributesTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends TelemetryProcessorsNonStringAttributesTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends TelemetryProcessorsNonStringAttributesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TelemetryProcessorsNonStringAttributesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends TelemetryProcessorsNonStringAttributesTest {}
}
