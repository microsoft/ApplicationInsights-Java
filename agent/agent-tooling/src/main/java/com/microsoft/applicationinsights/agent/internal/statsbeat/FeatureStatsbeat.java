// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.StatsbeatTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureStatsbeat extends BaseStatsbeat {

  private static final String FEATURE_METRIC_NAME = "Feature";

  private final Set<Feature> featureList = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final Set<String> instrumentationList =
      Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final FeatureType type;

  FeatureStatsbeat(CustomDimensions customDimensions, FeatureType type) {
    // track java distribution
    super(customDimensions);
    this.type = type;
    String javaVendor = System.getProperty("java.vendor");
    featureList.add(Feature.fromJavaVendor(javaVendor));
  }

  /** Returns a long that represents a list of features enabled. Each bitfield maps to a feature. */
  long getFeature() {
    return Feature.encode(featureList);
  }

  /**
   * Returns a long that represents a list of instrumentations. Each bitfield maps to an
   * instrumentation.
   */
  long[] getInstrumentation() {
    return Instrumentations.encode(instrumentationList);
  }

  // this is used by Exporter
  public void addInstrumentation(String instrumentation) {
    instrumentationList.add(instrumentation);
  }

  // this is used by ByteCodeUtil
  public void track2xBridgeUsage() {
    featureList.add(Feature.SDK_2X_BRIDGE_VIA_3X_AGENT);
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    String featureType;
    String featureValue = "";

    if (type == FeatureType.FEATURE) {
      featureValue = String.valueOf(getFeature());
      featureType = "0";
    } else {
      long[] encodedLongArray = getInstrumentation();
      if (encodedLongArray.length == 1) {
        featureValue = String.valueOf(encodedLongArray[0]);
      } else if (encodedLongArray.length == 2) {
        featureValue = encodedLongArray[0] + "," + encodedLongArray[1];
      }
      featureType = "1";
    }

    StatsbeatTelemetryBuilder telemetryBuilder =
        createStatsbeatTelemetry(telemetryClient, FEATURE_METRIC_NAME, 0);
    telemetryBuilder.addProperty("feature", featureValue);
    telemetryBuilder.addProperty("type", featureType);

    telemetryClient.trackStatsbeatAsync(telemetryBuilder.build());
  }

  void trackConfigurationOptions(Configuration config) {
    if (config.preview.authentication.enabled) {
      featureList.add(Feature.AAD);
    }
    if (config.preview.legacyRequestIdPropagation.enabled) {
      featureList.add(Feature.LEGACY_PROPAGATION_ENABLED);
    }

    // disabled instrumentations
    if (!config.instrumentation.azureSdk.enabled) {
      featureList.add(Feature.AZURE_SDK_DISABLED);
    }
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
    if (!config.instrumentation.quartz.enabled) {
      featureList.add(Feature.QUARTZ_DISABLED);
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
    if (!config.preview.instrumentation.akka.enabled) {
      featureList.add(Feature.AKKA_DISABLED);
    }
    if (!config.preview.instrumentation.apacheCamel.enabled) {
      featureList.add(Feature.APACHE_CAMEL_DISABLED);
    }
    if (config.preview.instrumentation.grizzly.enabled) {
      featureList.add(Feature.GRIZZLY_ENABLED);
    }
    if (!config.preview.instrumentation.play.enabled) {
      featureList.add(Feature.PLAY_DISABLED);
    }
    if (!config.preview.instrumentation.springIntegration.enabled) {
      featureList.add(Feature.SPRING_INTEGRATION_DISABLED);
    }
    if (!config.preview.instrumentation.vertx.enabled) {
      featureList.add(Feature.VERTX_DISABLED);
    }
    if (!config.preview.instrumentation.jaxrsAnnotations.enabled) {
      featureList.add(Feature.JAXRS_ANNOTATIONS_DISABLED);
    }

    // Statsbeat
    if (config.preview.statsbeat.disabled) {
      featureList.add(Feature.STATSBEAT_DISABLED);
    }

    if (config.preview.disablePropagation) {
      featureList.add(Feature.PROPAGATION_DISABLED);
    }
    if (!config.preview.captureHttpServer4xxAsError) {
      featureList.add(Feature.CAPTURE_HTTP_SERVER_4XX_AS_SUCCESS);
    }
    if (!config.preview.captureHttpServerHeaders.requestHeaders.isEmpty()
        || !config.preview.captureHttpServerHeaders.responseHeaders.isEmpty()) {
      featureList.add(Feature.CAPTURE_HTTP_SERVER_HEADERS);
    }
    if (!config.preview.captureHttpClientHeaders.requestHeaders.isEmpty()
        || !config.preview.captureHttpClientHeaders.responseHeaders.isEmpty()) {
      featureList.add(Feature.CAPTURE_HTTP_CLIENT_HEADERS);
    }
    if (!config.preview.processors.isEmpty()) {
      featureList.add(Feature.TELEMETRY_PROCESSOR_ENABLED);
    }
    if (config.preview.profiler.enabled) {
      featureList.add(Feature.PROFILER_ENABLED);
    }

    // customDimensions
    if (!config.customDimensions.isEmpty()) {
      featureList.add(Feature.CUSTOM_DIMENSIONS_ENABLED);
    }

    if (config.preview.captureLoggingLevelAsCustomDimension) {
      featureList.add(Feature.LOGGING_LEVEL_CUSTOM_PROPERTY_ENABLED);
    }
  }
}
