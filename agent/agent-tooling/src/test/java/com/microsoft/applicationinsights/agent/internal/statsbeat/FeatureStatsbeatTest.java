// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import java.util.BitSet;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

public class FeatureStatsbeatTest {

  @Test
  public void testAadEnabled() {
    testFeatureTrackingEnablement(
        (config, value) -> config.preview.authentication.enabled = value, Feature.AAD);
  }

  @Test
  public void testLegacyPropagationEnabled() {
    testFeatureTrackingEnablement(
        (config, value) -> config.preview.legacyRequestIdPropagation.enabled = value,
        Feature.LEGACY_PROPAGATION_ENABLED);
  }

  @Test
  public void testCassandraEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.cassandra.enabled = enabled,
        Feature.CASSANDRA_DISABLED);
  }

  @Test
  public void testJdbcEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.jdbc.enabled = enabled, Feature.JDBC_DISABLED);
  }

  @Test
  public void testJmsEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.jms.enabled = enabled, Feature.JMS_DISABLED);
  }

  @Test
  public void testKafkaEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.kafka.enabled = enabled,
        Feature.KAFKA_DISABLED);
  }

  @Test
  public void testMicrometerEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.micrometer.enabled = enabled,
        Feature.MICROMETER_DISABLED);
  }

  @Test
  public void testMongoEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.mongo.enabled = enabled,
        Feature.MONGO_DISABLED);
  }

  @Test
  public void testRedisEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.redis.enabled = enabled,
        Feature.REDIS_DISABLED);
  }

  @Test
  public void testSpringSchedulingEnabled() {
    testFeatureTrackingDisablement(
        (config, enabled) -> config.instrumentation.springScheduling.enabled = enabled,
        Feature.SPRING_SCHEDULING_DISABLED);
  }

  @Test
  public void testAddInstrumentationFirstLong() {
    FeatureStatsbeat instrumentationStatsbeat =
        new FeatureStatsbeat(new CustomDimensions(), FeatureType.INSTRUMENTATION);
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.jdbc");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.tomcat-7.0");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.http-url-connection");
    long[] expectedLongArray = new long[1];
    expectedLongArray[0] =
        (long)
            (Math.pow(2, 5)
                + Math.pow(2, 13)
                + Math.pow(
                    2,
                    21)); // Exponents are keys from StatsbeatTestUtils.INSTRUMENTATION_MAP_DECODING
    assertThat(instrumentationStatsbeat.getInstrumentation()).isEqualTo(expectedLongArray);
  }

  @Test
  public void testAddInstrumentationToSecondLongOnly() {
    FeatureStatsbeat instrumentationStatsbeat =
        new FeatureStatsbeat(new CustomDimensions(), FeatureType.INSTRUMENTATION);
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.undertow-1.4");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.play-ws-2.0");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.vertx-kafka-client-3.5");
    long[] expectedLongArray = new long[2];
    expectedLongArray[0] = 0;
    expectedLongArray[1] =
        (long)
            (Math.pow(2, 67 - 64)
                + Math.pow(2, 69 - 64)
                + Math.pow(2, 71 - 64)); // Exponents are keys from
    // StatsbeatTestUtils.INSTRUMENTATION_MAP_DECODING - 1
    assertThat(instrumentationStatsbeat.getInstrumentation()).isEqualTo(expectedLongArray);
  }

  @Test
  public void testAddInstrumentationToBoth() {
    FeatureStatsbeat instrumentationStatsbeat =
        new FeatureStatsbeat(new CustomDimensions(), FeatureType.INSTRUMENTATION);
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.undertow-1.4");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.play-ws-2.0");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.vertx-kafka-client-3.5");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.jdbc");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.tomcat-7.0");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.http-url-connection");
    long[] expectedLongArray = new long[2];
    expectedLongArray[0] = (long) (Math.pow(2, 5) + Math.pow(2, 13) + Math.pow(2, 21));
    expectedLongArray[1] =
        (long) (Math.pow(2, 67 - 64) + Math.pow(2, 69 - 64) + Math.pow(2, 71 - 64));
    assertThat(instrumentationStatsbeat.getInstrumentation()).isEqualTo(expectedLongArray);
  }

  private static void testFeatureTrackingEnablement(
      BiConsumer<Configuration, Boolean> init, Feature feature) {
    testFeature(init, feature, false, false);
    testFeature(init, feature, true, true);
  }

  private static void testFeatureTrackingDisablement(
      BiConsumer<Configuration, Boolean> init, Feature feature) {
    testFeature(init, feature, false, true);
    testFeature(init, feature, true, false);
  }

  private static void testFeature(
      BiConsumer<Configuration, Boolean> init,
      Feature feature,
      boolean configValue,
      boolean featureValue) {
    // given
    FeatureStatsbeat featureStatsbeat =
        new FeatureStatsbeat(new CustomDimensions(), FeatureType.FEATURE);

    Configuration config = newConfiguration();
    init.accept(config, configValue);

    // when
    featureStatsbeat.trackConfigurationOptions(config);

    // then
    assertThat(getBitAtIndex(featureStatsbeat.getFeature(), feature.getBitmapIndex()))
        .isEqualTo(featureValue);
  }

  private static Configuration newConfiguration() {
    Configuration config = new Configuration();
    config.instrumentation = new Configuration.Instrumentation();
    config.preview = new Configuration.PreviewConfiguration();
    config.preview.instrumentation = new Configuration.PreviewInstrumentation();
    // preview instrumentation is disabled by default
    config.preview.instrumentation.springIntegration.enabled = true;
    return config;
  }

  private static boolean getBitAtIndex(long feature, int index) {
    BitSet bitSet = BitSet.valueOf(new long[] {feature});
    return bitSet.get(index);
  }
}
