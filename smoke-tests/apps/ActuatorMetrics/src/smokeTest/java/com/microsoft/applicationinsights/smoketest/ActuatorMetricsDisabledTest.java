// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(JAVA_8)
@UseAgent("disabled_applicationinsights.json")
class ActuatorMetricsDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 1);

    // sleep a bit and make sure no micrometer metrics are reported
    Thread.sleep(10000);
    assertThat(testing.mockedIngestion.getItemsEnvelopeDataType("MetricData"))
        .noneMatch(ActuatorMetricsTest::isMicrometerMetric);
  }
}
