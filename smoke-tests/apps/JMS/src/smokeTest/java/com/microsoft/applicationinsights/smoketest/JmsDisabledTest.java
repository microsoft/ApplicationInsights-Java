// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(JAVA_8)
@UseAgent("disabled_applicationinsights.json")
class JmsDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/sendMessage")
  void doMostBasicTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    assertThat(rd.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd.getResponseCode()).isEqualTo("200");
    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd.getSuccess()).isTrue();

    // verify the downstream http dependency that is no longer part of the same trace
    List<Envelope> rddList = testing.mockedIngestion.waitForItems("RemoteDependencyData", 1);
    Envelope rddEnvelope = rddList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();

    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getName()).isEqualTo("GET /");
    assertThat(rdd.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd.getSuccess()).isTrue();

    // sleep a bit and make sure no kafka "requests" or dependencies are reported
    Thread.sleep(5000);
    assertThat(testing.mockedIngestion.getCountForType("RequestData")).isEqualTo(1);
    assertThat(testing.mockedIngestion.getCountForType("RemoteDependencyData")).isEqualTo(1);
  }
}
