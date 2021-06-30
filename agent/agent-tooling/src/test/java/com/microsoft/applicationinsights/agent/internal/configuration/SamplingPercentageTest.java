/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
    assertThat(roundToNearest(0.1f)).isEqualTo(0.1f);
    assertThat(roundToNearest(0.001f)).isEqualTo(0.001f);
    assertThat(roundToNearest(0)).isEqualTo(0);

    // imperfect
    assertThat(roundToNearest(90)).isEqualTo(100);
    assertThat(roundToNearest(51)).isEqualTo(50);
    assertThat(roundToNearest(49)).isEqualTo(50);
    assertThat(roundToNearest(34)).isCloseTo(33.333f, offset(0.001f));
    assertThat(roundToNearest(33)).isCloseTo(33.333f, offset(0.001f));
    assertThat(roundToNearest(26)).isEqualTo(25);
    assertThat(roundToNearest(24)).isEqualTo(25);
  }
}
