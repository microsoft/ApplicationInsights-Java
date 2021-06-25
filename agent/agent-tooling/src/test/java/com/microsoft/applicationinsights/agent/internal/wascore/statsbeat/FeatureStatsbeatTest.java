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

package com.microsoft.applicationinsights.agent.internal.wascore.statsbeat;

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
    featureStatsbeat.trackAadEnabled(true);

    features.add(Feature.AAD);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testAadDisable() {
    featureStatsbeat.trackAadEnabled(false);

    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testCassandraDisable() {
    featureStatsbeat.trackDisabledInstrumentations(false, true, true, true, true, true, true, true);

    features.add(Feature.Cassandra_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testJdbcDisable() {
    featureStatsbeat.trackDisabledInstrumentations(true, false, true, true, true, true, true, true);

    features.add(Feature.JDBC_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testJmsDisable() {
    featureStatsbeat.trackDisabledInstrumentations(true, true, false, true, true, true, true, true);

    features.add(Feature.JMS_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testKafkaDisable() {
    featureStatsbeat.trackDisabledInstrumentations(true, true, true, false, true, true, true, true);

    features.add(Feature.KAFKA_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testMicrometerDisable() {
    featureStatsbeat.trackDisabledInstrumentations(true, true, true, true, false, true, true, true);

    features.add(Feature.MICROMETER_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testMongoDisable() {
    featureStatsbeat.trackDisabledInstrumentations(true, true, true, true, true, false, true, true);

    features.add(Feature.MONGO_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testRedisDisable() {
    featureStatsbeat.trackDisabledInstrumentations(true, true, true, true, true, true, false, true);

    features.add(Feature.REDIS_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }

  @Test
  public void testSpringSchedulingDisable() {
    featureStatsbeat.trackDisabledInstrumentations(true, true, true, true, true, true, true, false);

    features.add(Feature.SPRING_SCHEDULING_DISABLED);
    assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
  }
}
