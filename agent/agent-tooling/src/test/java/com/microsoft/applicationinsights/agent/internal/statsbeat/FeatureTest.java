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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class FeatureTest {

  private static final Set<Feature> features;

  static {
    features = new HashSet<>();
    features.add(Feature.JAVA_VENDOR_ZULU);
    features.add(Feature.AAD);
    features.add(Feature.AZURE_SDK_DISABLED);
    features.add(Feature.JDBC_DISABLED);
    features.add(Feature.SPRING_INTEGRATION_DISABLED);
    features.add(Feature.STATSBEAT_DISABLED);
    features.add(Feature.JAVA_VERSION);
  }

  private static final long EXPECTED_FEATURE =
      (long)
          (Math.pow(2, 1)
              + Math.pow(2, 6)
              + Math.pow(2, 8)
              + Math.pow(2, 15)
              + Math.pow(2, 17)
              + Math.pow(
                  2, 20) // Exponents are keys from StatsbeatTestUtils.FEATURE_MAP_DECODING.)
              + Math.pow(2, 26));

  @Test
  public void tesEncodeAndDecodeFeature() {
    long number = Feature.encode(features);
    assertThat(number).isEqualTo(EXPECTED_FEATURE);
    Set<Feature> result = StatsbeatTestUtils.decodeFeature(number);
    assertThat(result).isEqualTo(features);
  }
}
