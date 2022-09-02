// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import static com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder.roundToNearest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

class SamplingPercentageTest {

  @Test
  void testRoundToNearest() {

    // perfect
    assertThat(roundToNearest(100)).isEqualTo(100);
    assertThat(roundToNearest(50)).isEqualTo(50);
    assertThat(roundToNearest(10)).isEqualTo(10);
    assertThat(roundToNearest(2)).isEqualTo(2);
    assertThat(roundToNearest(0.1)).isEqualTo(0.1);
    assertThat(roundToNearest(0.001)).isEqualTo(0.001);
    assertThat(roundToNearest(0)).isEqualTo(0);

    // imperfect
    assertThat(roundToNearest(90)).isEqualTo(100);
    assertThat(roundToNearest(51)).isEqualTo(50);
    assertThat(roundToNearest(49)).isEqualTo(50);
    assertThat(roundToNearest(34)).isCloseTo(33.333, offset(0.001));
    assertThat(roundToNearest(33)).isCloseTo(33.333, offset(0.001));
    assertThat(roundToNearest(26)).isEqualTo(25);
    assertThat(roundToNearest(24)).isEqualTo(25);
  }
}
