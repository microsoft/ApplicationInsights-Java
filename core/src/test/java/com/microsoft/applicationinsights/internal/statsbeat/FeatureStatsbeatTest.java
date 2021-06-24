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

package com.microsoft.applicationinsights.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FeatureStatsbeatTest {

  private FeatureStatsbeat featureStatsbeat;
  private String javaVendor;

  @BeforeEach
  public void init() {
    featureStatsbeat = new FeatureStatsbeat(new CustomDimensions());
    javaVendor = System.getProperty("java.vendor");
  }

  @Test
  public void testFeatureList() {
    Set<Feature> features = Collections.singleton(Feature.fromJavaVendor(javaVendor));
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testAadEnable() {
    featureStatsbeat.trackAadOn(true);
    Set<Feature> features = new HashSet<>();
    features.add(Feature.fromJavaVendor(javaVendor));
    features.add(Feature.AAD);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testAadDisable() {
    featureStatsbeat.trackAadOn(false);
    Set<Feature> features = new HashSet<>();
    features.add(Feature.fromJavaVendor(javaVendor));
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }
}
