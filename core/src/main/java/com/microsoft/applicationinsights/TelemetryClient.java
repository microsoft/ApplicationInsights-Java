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

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.extensibility.context.InternalContext;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;
import com.microsoft.applicationinsights.internal.util.ChannelFetcher;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SessionState;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

// Created by gupele
/**
 * Create an instance of this class to send telemetry to Azure Application Insights. General
 * overview
 * https://docs.microsoft.com/azure/application-insights/app-insights-api-custom-events-metrics
 */
public class TelemetryClient {
  private static final Object TELEMETRY_STOP_HOOK_LOCK = new Object();
  private static final Object TELEMETRY_CONTEXT_LOCK = new Object();
  private static AtomicLong generateCounter = new AtomicLong(0);
  private final TelemetryConfiguration configuration;
  private TelemetryContext context;
  private TelemetryChannel channel;

  /**
   * Initializes a new instance of the TelemetryClient class. Send telemetry with the specified
   * configuration.
   *
   * @param configuration The configuration this instance will work with.
   */
  public TelemetryClient(TelemetryConfiguration configuration) {
    if (configuration == null) {
      configuration = TelemetryConfiguration.getActive();
    }

    synchronized (TELEMETRY_STOP_HOOK_LOCK) {
      SDKShutdownActivity.INSTANCE.register(new TelemetryClientChannelFetcher());
    }

    this.configuration = configuration;
  }
  /**
   * Initializes a new instance of the TelemetryClient class, configured from the active
   * configuration.
   */
  public TelemetryClient() {
    this(TelemetryConfiguration.getActive());
  }

  /**
   * Gets the current context that is used to augment telemetry you send.
   *
   * @return A telemetry context used for all records. Changes to it will impact all future
   *     telemetry in this application session.
   */
  public TelemetryContext getContext() {
    if (context == null) {
      // lock and recheck there is still no initialized context. If so, create one.
      synchronized (TELEMETRY_CONTEXT_LOCK) {
        if (context == null) {
          context = createInitializedContext();
        }
      }
    }

    return context;
  }

  /**
   * Checks whether tracking is enabled.
   *
   * @return 'true' if tracking is disabled, 'false' otherwise.
   */
  public boolean isDisabled() {
    return (Strings.isNullOrEmpty(configuration.getInstrumentationKey())
            && Strings.isNullOrEmpty(getContext().getInstrumentationKey()))
        || configuration.isTrackingDisabled();
  }

  /**
   * Sends the specified state of a user session to Application Insights using {@link
   * #trackEvent(String name)} as this method will be deprecated.
   *
   * @param sessionState {@link com.microsoft.applicationinsights.telemetry.SessionState} value
   *     indicating the state of a user session.
   * @deprecated This method will be deprecated in version 2.0.0 of the Java SDK.
   */
  @Deprecated
  public void trackSessionState(SessionState sessionState) {
    this.trackEvent("Track Session State: " + sessionState.toString());
  }

  /**
   * Sends a custom event record to Application Insights. Appears in custom events in Analytics,
   * Search and Metrics Explorer.
   *
   * @param name A name for the event. Max length 150.
   * @param properties Named string values you can use to search and filter events.
   * @param metrics Numeric measurements associated with this event. Appear under Custom Metrics in
   *     Metrics Explorer.
   */
  public void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
    if (isDisabled()) {
      return;
    }

    if (Strings.isNullOrEmpty(name)) {
      name = "";
    }

    EventTelemetry et = new EventTelemetry(name);

    if (properties != null && properties.size() > 0) {
      MapUtil.copy(properties, et.getContext().getProperties());
    }

    if (metrics != null && metrics.size() > 0) {
      MapUtil.copy(metrics, et.getMetrics());
    }

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
      String message, SeverityLevel severityLevel, Map<String, String> properties) {
    if (isDisabled()) {
      return;
    }

    if (Strings.isNullOrEmpty(message)) {
      message = "";
    }

    TraceTelemetry et = new TraceTelemetry(message, severityLevel);

    if (properties != null && properties.size() > 0) {
      MapUtil.copy(properties, et.getContext().getProperties());
    }

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
   * @param value The value of the metric. Average if based on more than one sample count. Should be
   *     greater than 0.
   * @param sampleCount The sample count.
   * @param min The minimum value of the sample.
   * @param max The maximum value of the sample.
   * @param properties Named string values you can use to search and classify trace messages.
   */
  public void trackMetric(
      String name,
      double value,
      int sampleCount,
      double min,
      double max,
      Map<String, String> properties) {
    if (isDisabled()) {
      return;
    }

    if (Strings.isNullOrEmpty(name)) {
      name = "";
    }

    MetricTelemetry mt = new MetricTelemetry(name, value);
    mt.setCount(sampleCount);
    if (sampleCount > 1) {
      mt.setMin(min);
      mt.setMax(max);
    }

    if (properties != null && properties.size() > 0) {
      MapUtil.copy(properties, mt.getContext().getProperties());
    }

    this.track(mt);
  }

  /**
   * Sends a numeric metric to Application Insights. Appears in customMetrics in Analytics, and
   * under Custom Metrics in Metric Explorer.
   *
   * @param name The name of the metric. Max length 150.
   * @param value The value of the metric. Should be greater than 0.
   */
  public void trackMetric(String name, double value) {
    trackMetric(name, value, 1, value, value, null);
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
      Exception exception, Map<String, String> properties, Map<String, Double> metrics) {
    if (isDisabled()) {
      return;
    }

    ExceptionTelemetry et = new ExceptionTelemetry(exception);

    if (properties != null && properties.size() > 0) {
      MapUtil.copy(properties, et.getContext().getProperties());
    }

    if (metrics != null && metrics.size() > 0) {
      MapUtil.copy(metrics, et.getMetrics());
    }

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
    // Avoid creation of data if not needed
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
   * This method is part of the Application Insights infrastructure. Do not call it directly.
   *
   * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
   */
  public void track(Telemetry telemetry) {

    if (generateCounter.incrementAndGet() % 10000 == 0) {
      InternalLogger.INSTANCE.info("Total events generated till now %d", generateCounter.get());
    }

    if (telemetry == null) {
      throw new IllegalArgumentException("telemetry item cannot be null");
    }

    if (isDisabled()) {
      return;
    }

    if (telemetry.getTimestamp() == null) {
      telemetry.setTimestamp(new Date());
    }

    TelemetryContext ctx = this.getContext();

    if (Strings.isNullOrEmpty(ctx.getInstrumentationKey())) {
      ctx.setInstrumentationKey(configuration.getInstrumentationKey());
    }

    try {
      telemetry.getContext().initialize(ctx);
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        InternalLogger.INSTANCE.error(
            "Exception while telemetry context's initialization: '%s'", t.toString());
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }

    activateInitializers(telemetry);

    if (Strings.isNullOrEmpty(telemetry.getContext().getInstrumentationKey())) {
      throw new IllegalArgumentException("Instrumentation key cannot be undefined.");
    }

    if (!activateProcessors(telemetry)) {
      return;
    }

    try {
      QuickPulseDataCollector.INSTANCE.add(telemetry);
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
    }

    try {
      getChannel().send(telemetry);
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        InternalLogger.INSTANCE.error("Exception while sending telemetry: '%s'", t.toString());
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }
  }

  private void activateInitializers(Telemetry telemetry) {
    for (TelemetryInitializer initializer : this.configuration.getTelemetryInitializers()) {
      try {
        initializer.initialize(telemetry);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable e) {
        try {
          InternalLogger.INSTANCE.error(
              "Failed during telemetry initialization class '%s', exception: %s",
              initializer.getClass().getName(), e.toString());
        } catch (ThreadDeath td) {
          throw td;
        } catch (Throwable t2) {
          // chomp
        }
      }
    }
  }

  private boolean activateProcessors(Telemetry telemetry) {
    for (TelemetryProcessor processor : configuration.getTelemetryProcessors()) {
      try {
        if (!processor.process(telemetry)) {
          return false;
        }
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t) {
        try {
          InternalLogger.INSTANCE.error("Exception while processing telemetry: '%s'", t.toString());
        } catch (ThreadDeath td) {
          throw td;
        } catch (Throwable t2) {
          // chomp
        }
      }
    }

    return true;
  }

  /**
   * Flushes possible pending Telemetries in the channel. Not required for a continuously-running
   * server application.
   */
  public void flush() {
    getChannel().flush();
  }

  /** Gets the channel used by the client. */
  TelemetryChannel getChannel() {
    if (channel == null) {
      this.channel = configuration.getChannel();
    }

    return this.channel;
  }

  private TelemetryContext createInitializedContext() {
    TelemetryContext ctx = new TelemetryContext();
    ctx.setInstrumentationKey(configuration.getInstrumentationKey());
    for (ContextInitializer init : configuration.getContextInitializers()) {
      try {
        init.initialize(ctx);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t) {
        try {
          InternalLogger.INSTANCE.error("Exception in context initializer: '%s'", t.toString());
        } catch (ThreadDeath td) {
          throw td;
        } catch (Throwable t2) {
          // chomp
        }
      }
    }

    // Set the nodeName for billing purpose if it does not already exist
    InternalContext internal = ctx.getInternal();
    if (CommonUtils.isNullOrEmpty(internal.getNodeName())) {
      String host = CommonUtils.getHostName();
      if (!CommonUtils.isNullOrEmpty(host)) {
        internal.setNodeName(host);
      }
    }
    return ctx;
  }

  private final class TelemetryClientChannelFetcher implements ChannelFetcher {
    public TelemetryChannel fetch() {
      return getChannel();
    }
  }
}
