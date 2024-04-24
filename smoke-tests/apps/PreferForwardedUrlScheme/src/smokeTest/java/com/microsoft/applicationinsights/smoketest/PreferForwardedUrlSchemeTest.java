// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
public abstract class PreferForwardedUrlSchemeTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().setHttpHeader("X-Forwarded-Proto", "https").build();

  @Test
  @TargetUri("/test")
  void test() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test");
    // note: this is normally http://, but here is https:// because of the
    // support for X-Forwarded-Proto
    assertThat(telemetry.rd.getUrl()).matches("https://localhost:[0-9]+/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();
  }

  @Environment(JAVA_8)
  static class Java8Test extends PreferForwardedUrlSchemeTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends PreferForwardedUrlSchemeTest {}

  @Environment(JAVA_11)
  static class Java11Test extends PreferForwardedUrlSchemeTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends PreferForwardedUrlSchemeTest {}

  @Environment(JAVA_17)
  static class Java17Test extends PreferForwardedUrlSchemeTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends PreferForwardedUrlSchemeTest {}

  @Environment(JAVA_21)
  static class Java21Test extends PreferForwardedUrlSchemeTest {}

  @Environment(JAVA_21_OPENJ9)
  static class Java21OpenJ9Test extends PreferForwardedUrlSchemeTest {}
}
