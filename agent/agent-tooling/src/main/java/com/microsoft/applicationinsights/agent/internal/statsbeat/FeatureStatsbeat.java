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

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import java.util.HashSet;
import java.util.Set;

class FeatureStatsbeat extends BaseStatsbeat {

  private static final String FEATURE_METRIC_NAME = "Feature";

  private final Set<Feature> featureList = new HashSet<>(64);

  FeatureStatsbeat(CustomDimensions customDimensions) {
    // track java distribution
    super(customDimensions);
    String javaVendor = System.getProperty("java.vendor");
    featureList.add(Feature.fromJavaVendor(javaVendor));
  }

  /** Returns a long that represents a list of features enabled. Each bitfield maps to a feature. */
  long getFeature() {
    return Feature.encode(featureList);
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    TelemetryItem statsbeatTelemetry =
        createStatsbeatTelemetry(telemetryClient, FEATURE_METRIC_NAME, 0);
    TelemetryUtil.getProperties(statsbeatTelemetry.getData().getBaseData())
        .put("feature", String.valueOf(getFeature()));
    telemetryClient.trackStatsbeatAsync(statsbeatTelemetry);
  }

  void trackConfigurationOptions(Configuration config) {
    if (config.preview.authentication.enabled) {
      featureList.add(Feature.AAD);
    }
    if (!config.preview.legacyRequestIdPropagation.enabled) {
      featureList.add(Feature.LEGACY_PROPAGATION_DISABLED);
    }

    // disabled instrumentations
    if (!config.instrumentation.cassandra.enabled) {
      featureList.add(Feature.CASSANDRA_DISABLED);
    }
    if (!config.instrumentation.jdbc.enabled) {
      featureList.add(Feature.JDBC_DISABLED);
    }
    if (!config.instrumentation.jms.enabled) {
      featureList.add(Feature.JMS_DISABLED);
    }
    if (!config.instrumentation.kafka.enabled) {
      featureList.add(Feature.KAFKA_DISABLED);
    }
    if (!config.instrumentation.micrometer.enabled) {
      featureList.add(Feature.MICROMETER_DISABLED);
    }
    if (!config.instrumentation.mongo.enabled) {
      featureList.add(Feature.MONGO_DISABLED);
    }
    if (!config.instrumentation.rabbitmq.enabled) {
      featureList.add(Feature.RABBITMQ_DISABLED);
    }
    if (!config.instrumentation.redis.enabled) {
      featureList.add(Feature.REDIS_DISABLED);
    }
    if (!config.instrumentation.springScheduling.enabled) {
      featureList.add(Feature.SPRING_SCHEDULING_DISABLED);
    }

    // preview instrumentation
    if (!config.preview.instrumentation.azureSdk.enabled) {
      featureList.add(Feature.AZURE_SDK_DISABLED);
    }
    if (!config.preview.instrumentation.grizzly.enabled) {
      featureList.add(Feature.GRIZZLY_DISABLED);
    }
    if (!config.preview.instrumentation.springIntegration.enabled) {
      featureList.add(Feature.SPRING_INTEGRATION_DISABLED);
    }
  }
}
