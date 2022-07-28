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

package com.microsoft.applicationinsights;

import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;

public class TelemetryClient {

  private final TelemetryConfiguration configuration = new TelemetryConfiguration();
  private final TelemetryContext context = new TelemetryContext();

  public TelemetryClient() {}

  public TelemetryContext getContext() {
    return context;
  }

  public boolean isDisabled() {
    return configuration.isTrackingDisabled();
  }

  public void trackEvent(
      String name,
      @Nullable Map<String, String> properties,
      @Nullable Map<String, Double> metrics) {

    if (isDisabled()) {
      return;
    }

    if (name == null || name.isEmpty()) {
      name = "";
    }

    EventTelemetry et = new EventTelemetry(name);

    MapUtil.copy(properties, et.getContext().getProperties());
    MapUtil.copy(metrics, et.getMetrics());

    this.track(et);
  }

  /**
   * Sends a custom event record to Application Insights. Appears in "custom events" in Analytics,
   * Search and Metrics Explorer.
   *
   * @param name A name for the event. Max length 150.
   */
  public void trackEvent(String name) {
    trackEvent(name, null, null);
  }

  /**
   * Sends a custom event record to Application Insights. Appears in "custom events" in Analytics,
   * Search and Metrics Explorer.
   *
   * @param telemetry An event telemetry item.
   */
  public void trackEvent(EventTelemetry telemetry) {
    track(telemetry);
  }

  /**
   * Sends a TraceTelemetry record to Application Insights. Appears in "traces" in Analytics and
   * Search.
   *
   * @param message A log message. Max length 10000.
   * @param severityLevel The severity level.
   * @param properties Named string values you can use to search and classify trace messages.
   */
  public void trackTrace(
      String message,
      @Nullable SeverityLevel severityLevel,
      @Nullable Map<String, String> properties) {

    if (isDisabled()) {
      return;
    }

    if (LocalStringsUtils.isNullOrEmpty(message)) {
      message = "";
    }

    TraceTelemetry et = new TraceTelemetry(message, severityLevel);

    MapUtil.copy(properties, et.getContext().getProperties());

    this.track(et);
  }

  /**
   * Sends a TraceTelemetry record to Application Insights. Appears in "traces" in Analytics and
   * Search.
   *
   * @param message A log message. Max length 10000.
   */
  public void trackTrace(String message) {
    trackTrace(message, null, null);
  }

  /**
   * Sends a TraceTelemetry record. Appears in "traces" in Analytics and Search.
   *
   * @param message A log message. Max length 10000.
   * @param severityLevel The severity level.
   */
  public void trackTrace(String message, SeverityLevel severityLevel) {
    trackTrace(message, severityLevel, null);
  }

  /**
   * Sends a TraceTelemetry record for display in Diagnostic Search.
   *
   * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
   */
  public void trackTrace(TraceTelemetry telemetry) {
    this.track(telemetry);
  }

  /**
   * Sends a numeric metric to Application Insights. Appears in customMetrics in Analytics, and
   * under Custom Metrics in Metric Explorer.
   *
   * @param name The name of the metric. Max length 150.
   * @param value The value of the metric. Sum if it represents an aggregation.
   * @param sampleCount The sample count.
   * @param min The minimum value of the sample.
   * @param max The maximum value of the sample.
   * @param stdDev The standard deviation of the sample.
   * @param properties Named string values you can use to search and classify trace messages.
   * @throws IllegalArgumentException if name is null or empty
   */
  public void trackMetric(
      String name,
      double value,
      @Nullable Integer sampleCount,
      @Nullable Double min,
      @Nullable Double max,
      @Nullable Double stdDev,
      @Nullable Map<String, String> properties) {

    if (isDisabled()) {
      return;
    }

    MetricTelemetry mt = new MetricTelemetry(name, value);
    mt.setCount(sampleCount);
    mt.setMin(min);
    mt.setMax(max);
    mt.setStandardDeviation(stdDev);
    MapUtil.copy(properties, mt.getProperties());
    this.track(mt);
  }

  /**
   * Sends a numeric metric to Application Insights. Appears in customMetrics in Analytics, and
   * under Custom Metrics in Metric Explorer.
   *
   * @param name The name of the metric. Max length 150.
   * @param value The value of the metric.
   * @throws IllegalArgumentException if name is null or empty.
   */
  public void trackMetric(String name, double value) {
    trackMetric(name, value, null, null, null, null, null);
  }

  /**
   * Sends a numeric metric to Application Insights. Appears in customMetrics in Analytics, and
   * under Custom Metrics in Metric Explorer.
   *
   * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
   */
  public void trackMetric(MetricTelemetry telemetry) {
    track(telemetry);
  }

  /**
   * Sends an exception record to Application Insights. Appears in "exceptions" in Analytics and
   * Search.
   *
   * @param exception The exception to log information about.
   * @param properties Named string values you can use to search and classify trace messages.
   * @param metrics Measurements associated with this exception event. Appear in "custom metrics" in
   *     Metrics Explorer.
   */
  public void trackException(
      Exception exception,
      @Nullable Map<String, String> properties,
      @Nullable Map<String, Double> metrics) {

    if (isDisabled()) {
      return;
    }

    ExceptionTelemetry et = new ExceptionTelemetry(exception);

    MapUtil.copy(properties, et.getContext().getProperties());
    MapUtil.copy(metrics, et.getMetrics());

    this.track(et);
  }

  /**
   * Sends an exception record to Application Insights. Appears in "exceptions" in Analytics and
   * Search.
   *
   * @param exception The exception to log information about.
   */
  public void trackException(Exception exception) {
    trackException(exception, null, null);
  }

  /**
   * Sends an ExceptionTelemetry record for display in Diagnostic Search.
   *
   * @param telemetry An already constructed exception telemetry record.
   */
  public void trackException(ExceptionTelemetry telemetry) {
    track(telemetry);
  }

  /**
   * Sends a request record to Application Insights. Appears in "requests" in Search and Analytics,
   * and contributes to metric charts such as Server Requests, Server Response Time, Failed
   * Requests.
   *
   * @param name A user-friendly name for the request or operation.
   * @param timestamp The time of the request.
   * @param duration The duration, in milliseconds, of the request processing.
   * @param responseCode The HTTP response code.
   * @param success true to record the operation as a successful request, false as a failed request.
   */
  public void trackHttpRequest(
      String name, Date timestamp, long duration, String responseCode, boolean success) {

    if (isDisabled()) {
      return;
    }

    track(new RequestTelemetry(name, timestamp, duration, responseCode, success));
  }

  /**
   * Sends a request record to Application Insights. Appears in "requests" in Search and Analytics,
   * and contributes to metric charts such as Server Requests, Server Response Time, Failed
   * Requests.
   *
   * @param request request
   */
  public void trackRequest(RequestTelemetry request) {
    track(request);
  }

  public void trackDependency(
      String dependencyName, String commandName, Duration duration, boolean success) {

    RemoteDependencyTelemetry remoteDependencyTelemetry =
        new RemoteDependencyTelemetry(dependencyName, commandName, duration, success);

    trackDependency(remoteDependencyTelemetry);
  }

  /**
   * Sends a dependency record to Application Insights. Appears in "dependencies" in Search and
   * Analytics. Set device type == "PC" to have the record contribute to metric charts such as
   * Server Dependency Calls, Dependency Response Time, and Dependency Failures.
   *
   * @param telemetry telemetry
   */
  public void trackDependency(RemoteDependencyTelemetry telemetry) {

    if (isDisabled()) {
      return;
    }

    if (telemetry == null) {
      telemetry = new RemoteDependencyTelemetry("");
    }

    track(telemetry);
  }

  /**
   * Sends a page view record to Application Insights. Appears in "page views" in Search and
   * Analytics, and contributes to metric charts such as Page View Load Time.
   *
   * @param name The name of the page.
   */
  public void trackPageView(String name) {

    if (isDisabled()) {
      return;
    }

    if (name == null) {
      name = "";
    }

    Telemetry telemetry = new PageViewTelemetry(name);
    track(telemetry);
  }

  /**
   * Send information about the page viewed in the application.
   *
   * @param telemetry The telemetry to send
   */
  public void trackPageView(PageViewTelemetry telemetry) {
    track(telemetry);
  }

  /**
   * Flushes possible pending Telemetries. Not required for a continuously-running server
   * application.
   */
  public void flush() {
    // Javaagent provides implementation
  }

  /**
   * This method is part of the Application Insights infrastructure. Do not call it directly.
   *
   * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
   */
  public void track(Telemetry telemetry) {
    // Javaagent provides implementation
  }
}
