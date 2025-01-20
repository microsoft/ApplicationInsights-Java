// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(JAVA_8)
@UseAgent("disabled_applicationinsights.json")
class MicrometerDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test");
    assertThat(telemetry.rd.getSuccess()).isTrue();

    // sleep a bit and make sure no micrometer metrics are reported
    Thread.sleep(10000);
    assertThat(testing.mockedIngestion.getItemsEnvelopeDataType("MetricData"))
        .noneMatch(MicrometerTest::isMicrometerMetricWithValueOne);
  }
}
