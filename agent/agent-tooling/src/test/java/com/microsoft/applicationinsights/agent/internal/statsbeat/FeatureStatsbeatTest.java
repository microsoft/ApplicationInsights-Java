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
    testFeatureTrackingDisablement(
        (config, value) -> config.preview.legacyRequestIdPropagation.enabled = value,
        Feature.LEGACY_PROPAGATION_DISABLED);
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
  public void testAddInstrumentation() {
    FeatureStatsbeat instrumentationStatsbeat =
        new FeatureStatsbeat(new CustomDimensions(), FeatureType.INSTRUMENTATION);
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.jdbc");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.tomcat-7.0");
    instrumentationStatsbeat.addInstrumentation("io.opentelemetry.http-url-connection");
    assertThat(instrumentationStatsbeat.getInstrumentation())
        .isEqualTo(
            (long)
                (Math.pow(2, 13)
                    + Math.pow(2, 21)
                    + Math.pow(
                        2, 57))); // Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP
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
    config.preview.instrumentation.azureSdk.enabled = true;
    config.preview.instrumentation.springIntegration.enabled = true;
    return config;
  }

  private static boolean getBitAtIndex(long feature, int index) {
    BitSet bitSet = BitSet.valueOf(new long[] {feature});
    return bitSet.get(index);
  }
}
