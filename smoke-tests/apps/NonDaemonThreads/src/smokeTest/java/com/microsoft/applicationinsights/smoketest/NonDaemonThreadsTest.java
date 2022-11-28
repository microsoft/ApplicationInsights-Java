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
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// IMPORTANT this is also currently the only smoke test for logs that occur outside a request
@UseAgent
abstract class NonDaemonThreadsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/spawn-another-java-process")
  void spawnAnotherJavaProcess() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> rddList = testing.mockedIngestion.waitForItems("RemoteDependencyData", 1);
    List<Envelope> mdList = testing.mockedIngestion.waitForItems("MessageData", 1);

    Envelope rdEnvelope = rdList.get(0);
    Envelope rddEnvelope = rddList.get(0);
    Envelope mdEnvelope = mdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();
    assertThat(mdEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();
    MessageData md = (MessageData) ((Data<?>) mdEnvelope.getData()).getBaseData();

    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd.getSuccess()).isTrue();

    assertThat(rdd.getName()).isEqualTo("GET /search");
    assertThat(rdd.getType()).isEqualTo("Http");
    assertThat(rdd.getTarget()).isEqualTo("www.bing.com");
    assertThat(rdd.getData()).isEqualTo("https://www.bing.com/search?q=test");
    assertThat(rdd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd.getSuccess()).isTrue();

    assertThat(md.getMessage()).isEqualTo("done");
    assertThat(md.getSeverityLevel()).isEqualTo(SeverityLevel.INFORMATION);
    assertThat(md.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md.getProperties()).containsKey("ThreadName");
    assertThat(md.getProperties()).hasSize(3);

    assertThat(rddEnvelope.getTags()).doesNotContainKey("ai.operation.parentId");

    assertThat(mdEnvelope.getTags()).doesNotContainKey("ai.operation.id");
    assertThat(mdEnvelope.getTags()).doesNotContainKey("ai.operation.parentId");
  }

  @Environment(JAVA_8)
  static class Java8Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_11)
  static class Java11Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_17)
  static class Java17Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_19)
  static class Java18Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends NonDaemonThreadsTest {}

  @Environment(JAVA_20)
  static class Java19Test extends NonDaemonThreadsTest {}
}
