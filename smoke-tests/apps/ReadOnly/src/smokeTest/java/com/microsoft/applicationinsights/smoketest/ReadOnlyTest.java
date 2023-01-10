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
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class ReadOnlyTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().setReadOnly(true).setSkipHealthCheck(true).build();

  @Test
  void test() throws Exception {
    List<Envelope> mdList = testing.mockedIngestion.waitForItems("MessageData", 1);

    Envelope mdEnvelope = mdList.get(0);

    assertThat(mdEnvelope.getSampleRate()).isNull();

    MessageData md = (MessageData) ((Data<?>) mdEnvelope.getData()).getBaseData();

    assertThat(md.getMessage()).isEqualTo("hello");
    assertThat(md.getSeverityLevel()).isEqualTo(SeverityLevel.INFORMATION);
    assertThat(md.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md.getProperties()).containsKey("ThreadName");
    assertThat(md.getProperties()).hasSize(3);
  }

  @Environment(JAVA_8)
  static class Java8Test extends ReadOnlyTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends ReadOnlyTest {}

  @Environment(JAVA_11)
  static class Java11Test extends ReadOnlyTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends ReadOnlyTest {}

  @Environment(JAVA_17)
  static class Java17Test extends ReadOnlyTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends ReadOnlyTest {}

  @Environment(JAVA_19)
  static class Java18Test extends ReadOnlyTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends ReadOnlyTest {}

  @Environment(JAVA_20)
  static class Java19Test extends ReadOnlyTest {}
}
