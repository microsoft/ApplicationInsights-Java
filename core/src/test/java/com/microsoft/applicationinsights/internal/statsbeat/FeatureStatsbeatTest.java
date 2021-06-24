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

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FeatureStatsbeatTest {

  private FeatureStatsbeat featureStatsbeat;
  private Set<Feature> features;

  @BeforeEach
  public void init() {
    featureStatsbeat = new FeatureStatsbeat(new CustomDimensions());
    String javaVendor = System.getProperty("java.vendor");
    features = new HashSet<>();
    features.add(Feature.fromJavaVendor(javaVendor));
  }

  @Test
  public void testFeatureList() {
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testAadEnable() {
    featureStatsbeat.trackAadOn(true);

    features.add(Feature.AAD);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testAadDisable() {
    featureStatsbeat.trackAadOn(false);

    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testCassandraDisable() {
    featureStatsbeat.trackInstrumentationOff(false, true, true, true, true, true, true, true);

    features.add(Feature.Cassandra_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testJdbcDisable() {
    featureStatsbeat.trackInstrumentationOff(true, false, true, true, true, true, true, true);

    features.add(Feature.JDBC_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testJmsDisable() {
    featureStatsbeat.trackInstrumentationOff(true, true, false, true, true, true, true, true);

    features.add(Feature.JMS_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testKafkaDisable() {
    featureStatsbeat.trackInstrumentationOff(true, true, true, false, true, true, true, true);

    features.add(Feature.KAFKA_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testMicrometerDisable() {
    featureStatsbeat.trackInstrumentationOff(true, true, true, true, false, true, true, true);

    features.add(Feature.MICROMETER_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testMongoDisable() {
    featureStatsbeat.trackInstrumentationOff(true, true, true, true, true, false, true, true);

    features.add(Feature.MONGO_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testRedisDisable() {
    featureStatsbeat.trackInstrumentationOff(true, true, true, true, true, true, false, true);

    features.add(Feature.REDIS_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testSpringSchedulingDisable() {
    featureStatsbeat.trackInstrumentationOff(true, true, true, true, true, true, true, false);

    features.add(Feature.SPRING_SCHEDULING_OFF);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }
}
