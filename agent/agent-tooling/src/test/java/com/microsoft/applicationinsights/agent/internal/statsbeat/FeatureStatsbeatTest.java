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

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import java.util.BitSet;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FeatureStatsbeatTest {

  private FeatureStatsbeat featureStatsbeat;

  @BeforeEach
  public void init() {
    featureStatsbeat = new FeatureStatsbeat(new CustomDimensions());
  }

  @Test
  public void testAadEnable() {
    // when
    featureStatsbeat.trackAadEnabled(true);

    // then
    assertThat(getBitAtIndex(featureStatsbeat.getFeature(), Feature.AAD.getBitmapIndex()))
        .isEqualTo(true);
  }

  @Test
  public void testAadDisable() {
    // when
    featureStatsbeat.trackAadEnabled(false);

    // then
    assertThat(getBitAtIndex(featureStatsbeat.getFeature(), Feature.AAD.getBitmapIndex()))
        .isEqualTo(false);
  }

  @Test
  public void testCassandraDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.cassandra.enabled = false;
        },
        Feature.CASSANDRA_DISABLED,
        true);
  }

  @Test
  public void testJdbcDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.jdbc.enabled = false;
        },
        Feature.JDBC_DISABLED,
        true);
  }

  @Test
  public void testJmsDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.jms.enabled = false;
        },
        Feature.JMS_DISABLED,
        true);
  }

  @Test
  public void testKafkaDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.kafka.enabled = false;
        },
        Feature.KAFKA_DISABLED,
        true);
  }

  @Test
  public void testMicrometerDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.micrometer.enabled = false;
        },
        Feature.MICROMETER_DISABLED,
        true);
  }

  @Test
  public void testMongoDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.mongo.enabled = false;
        },
        Feature.MONGO_DISABLED,
        true);
  }

  @Test
  public void testRedisDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.redis.enabled = false;
        },
        Feature.REDIS_DISABLED,
        true);
  }

  @Test
  public void testSpringSchedulingDisable() {
    testDisabledInstrumentation(
        config -> {
          config.instrumentation.springScheduling.enabled = false;
        },
        Feature.SPRING_SCHEDULING_DISABLED,
        true);
  }

  private static void testDisabledInstrumentation(
      Consumer<Configuration> init, Feature expectedFeature, boolean expectedValue) {
    // given
    FeatureStatsbeat featureStatsbeat = new FeatureStatsbeat(new CustomDimensions());

    Configuration config = newConfiguration();
    init.accept(config);

    // when
    featureStatsbeat.trackDisabledInstrumentations(config);

    // then
    assertThat(getBitAtIndex(featureStatsbeat.getFeature(), expectedFeature.getBitmapIndex()))
        .isEqualTo(expectedValue);
  }

  private static Configuration newConfiguration() {
    Configuration config = new Configuration();
    config.instrumentation = new Configuration.Instrumentation();
    config.preview = new Configuration.PreviewConfiguration();
    config.preview.instrumentation = new Configuration.PreviewInstrumentation();
    // preview instrumentation is disabled by default
    config.preview.instrumentation.azureSdk.enabled = true;
    config.preview.instrumentation.springIntegration.enabled = true;
    return config;
  }

  private static boolean getBitAtIndex(long feature, int index) {
    BitSet bitSet = BitSet.valueOf(new long[] {feature});
    return bitSet.get(index);
  }
}
