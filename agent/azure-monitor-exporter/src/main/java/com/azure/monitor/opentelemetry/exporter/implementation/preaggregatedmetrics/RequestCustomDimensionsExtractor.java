package com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;

import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.DurationBucketizer.getPerformanceBucket;

public final class RequestCustomDimensionsExtractor {

  // visible for testing
  static final String REQUEST_METRIC_ID = "requests/duration";
  static final String MS_METRIC_ID = "_MS.metricId";
  static final String MS_IS_AUTOCOLLECTED = "_MS.IsAutocollected";
  static final String TRUE = "True";
  static final String FALSE = "False";
  static final String MS_PROCESSED_BY_METRIC_EXTRACTORS = "_MS.ProcessedByMetricExtractors";
  static final String PERFORMANCE_BUCKET = "request/performanceBucket";
  static final String REQUEST_RESULT_CODE = "request/resultCode";
  static final String OPERATION_SYNTHETIC = "operation/synthetic";
  static final String CLOUD_ROLE_NAME = "cloud/roleName";
  static final String CLOUD_ROLE_INSTANCE = "cloud/roleInstance";
  static final String REQUEST_SUCCESS = "request/success";

  public static void updatePreAggMetricsCustomDimensions(AbstractTelemetryBuilder metricTelemetryBuilder, double value, String resultCode) {
    metricTelemetryBuilder.addProperty(MS_METRIC_ID, REQUEST_METRIC_ID);
    metricTelemetryBuilder.addProperty(MS_IS_AUTOCOLLECTED, TRUE);
    // this flag will inform the ingestion service to stop post-aggregation
    metricTelemetryBuilder.addProperty(MS_PROCESSED_BY_METRIC_EXTRACTORS, TRUE);

    // TODO figure out the correct duration/value
    metricTelemetryBuilder.addProperty(PERFORMANCE_BUCKET, getPerformanceBucket(value));
    metricTelemetryBuilder.addProperty(REQUEST_RESULT_CODE, resultCode);
    metricTelemetryBuilder.addProperty(OPERATION_SYNTHETIC, FALSE);
    metricTelemetryBuilder.addProperty(CLOUD_ROLE_NAME, metricTelemetryBuilder.build().getTags().get(ContextTagKeys.AI_CLOUD_ROLE.toString()));
    metricTelemetryBuilder.addProperty(CLOUD_ROLE_INSTANCE, metricTelemetryBuilder.build().getTags().get(
        ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString()));
    metricTelemetryBuilder.addProperty(REQUEST_SUCCESS, TRUE);
  }

  private RequestCustomDimensionsExtractor() {}
}
