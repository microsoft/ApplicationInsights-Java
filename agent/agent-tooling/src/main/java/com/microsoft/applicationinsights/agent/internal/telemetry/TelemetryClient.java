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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.microsoft.applicationinsights.agent.internal.common.LocalFileSystemUtils;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.models.ContextTagKeys;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MessageData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricDataPoint;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorBase;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorDomain;
import com.microsoft.applicationinsights.agent.internal.exporter.models.PageViewData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RemoteDependencyData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RequestData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryEventData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryExceptionData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileCache;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileLoader;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileSender;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseDataCollector;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TelemetryClient {

  private static final String TELEMETRY_FOLDER = "telemetry";
  private static final String STATSBEAT_FOLDER = "statsbeat";

  /**
   * Windows: C:\Users\{USER_NAME}\AppData\Local\Temp\applicationinsights Linux:
   * /var/temp/applicationinsights We will store all persisted files in this folder for all apps.
   * TODO it is a good security practice to purge data after 24 hours in this folder.
   */
  private static final File DEFAULT_FOLDER =
      new File(LocalFileSystemUtils.getTempDir(), "applicationinsights");

  private static final String EVENT_TELEMETRY_NAME = "Event";
  private static final String EXCEPTION_TELEMETRY_NAME = "Exception";
  private static final String MESSAGE_TELEMETRY_NAME = "Message";
  private static final String METRIC_TELEMETRY_NAME = "Metric";
  private static final String PAGE_VIEW_TELEMETRY_NAME = "PageView";
  private static final String REMOTE_DEPENDENCY_TELEMETRY_NAME = "RemoteDependency";
  private static final String REQUEST_TELEMETRY_NAME = "Request";

  private static volatile @MonotonicNonNull TelemetryClient active;

  private final Set<String> nonFilterableMetricNames = new HashSet<>();

  private volatile @MonotonicNonNull String instrumentationKey;
  private volatile @MonotonicNonNull String roleName;
  private volatile @MonotonicNonNull String roleInstance;
  private volatile @MonotonicNonNull String statsbeatInstrumentationKey;

  private final EndpointProvider endpointProvider = new EndpointProvider();

  // globalTags contain:
  // * cloud role name
  // * cloud role instance
  // * sdk version
  // * application version (if provided in customDimensions)
  private final Map<String, String> globalTags;
  // contains customDimensions from json configuration
  private final Map<String, String> globalProperties;

  private final List<MetricFilter> metricFilters;

  private final Configuration.AadAuthentication aadAuthentication;

  private final Object channelInitLock = new Object();
  private volatile @MonotonicNonNull BatchSpanProcessor channelBatcher;
  private volatile @MonotonicNonNull BatchSpanProcessor statsbeatChannelBatcher;

  // only used by tests
  public TelemetryClient() {
    this(new HashMap<>(), new ArrayList<>(), null);
  }

  public TelemetryClient(
      Map<String, String> customDimensions,
      List<MetricFilter> metricFilters,
      Configuration.AadAuthentication aadAuthentication) {
    StringSubstitutor substitutor = new StringSubstitutor(System.getenv());
    Map<String, String> globalProperties = new HashMap<>();
    Map<String, String> globalTags = new HashMap<>();
    for (Map.Entry<String, String> entry : customDimensions.entrySet()) {
      String key = entry.getKey();
      if (key.equals("service.version")) {
        globalTags.put(
            ContextTagKeys.AI_APPLICATION_VER.toString(), substitutor.replace(entry.getValue()));
      } else {
        globalProperties.put(key, substitutor.replace(entry.getValue()));
      }
    }

    globalTags.put(
        ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString(),
        PropertyHelper.getQualifiedSdkVersionString());

    this.globalProperties = globalProperties;
    this.globalTags = globalTags;
    this.metricFilters = metricFilters;
    this.aadAuthentication = aadAuthentication;
  }

  public static TelemetryClient getActive() {
    if (active == null) {
      throw new IllegalStateException("agent was not initialized");
    }

    return active;
  }

  public static void setActive(TelemetryClient telemetryClient) {
    if (active != null) {
      throw new IllegalStateException("Already initialized");
    }
    TelemetryClient.active = telemetryClient;
  }

  public void trackAsync(TelemetryItem telemetry) {
    MonitorDomain data = telemetry.getData().getBaseData();
    if (data instanceof MetricsData) {
      MetricsData metricsData = (MetricsData) data;
      List<MetricDataPoint> filteredPoints =
          metricsData.getMetrics().stream()
              .filter(
                  point -> {
                    String metricName = point.getName();
                    if (nonFilterableMetricNames.contains(metricName)) {
                      return true;
                    }
                    for (MetricFilter metricFilter : metricFilters) {
                      if (!metricFilter.matches(metricName)) {
                        return false;
                      }
                    }
                    return true;
                  })
              .collect(Collectors.toList());

      if (filteredPoints.isEmpty()) {
        return;
      }
      metricsData.setMetrics(filteredPoints);
    }

    if (telemetry.getTime() == null) {
      // this is easy to forget when adding new telemetry
      throw new AssertionError("telemetry item is missing time");
    }

    QuickPulseDataCollector.INSTANCE.add(telemetry);

    TelemetryObservers.INSTANCE.getObservers().forEach(consumer -> consumer.accept(telemetry));

    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    getChannelBatcher().trackAsync(telemetry);
  }

  public void trackStatsbeatAsync(TelemetryItem telemetry) {
    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    getStatsbeatChannelBatcher().trackAsync(telemetry);
  }

  public CompletableResultCode flushChannelBatcher() {
    if (channelBatcher != null) {
      return channelBatcher.forceFlush();
    } else {
      return CompletableResultCode.ofSuccess();
    }
  }

  public BatchSpanProcessor getChannelBatcher() {
    if (channelBatcher == null) {
      synchronized (channelInitLock) {
        if (channelBatcher == null) {
          LocalFileCache localFileCache = new LocalFileCache();
          File telemetryFolder = getTelemetryFolder(TELEMETRY_FOLDER);
          LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache, telemetryFolder);
          LocalFileWriter localFileWriter = new LocalFileWriter(localFileCache, telemetryFolder);
          TelemetryChannel channel =
              TelemetryChannel.create(
                  endpointProvider.getIngestionEndpointUrl(), localFileWriter, aadAuthentication);
          LocalFileSender.start(localFileLoader, channel);
          channelBatcher = BatchSpanProcessor.builder(channel).build();
        }
      }
    }
    return channelBatcher;
  }

  public BatchSpanProcessor getStatsbeatChannelBatcher() {
    if (statsbeatChannelBatcher == null) {
      synchronized (channelInitLock) {
        if (statsbeatChannelBatcher == null) {
          LocalFileCache localFileCache = new LocalFileCache();
          File statsbeatFolder = getTelemetryFolder(STATSBEAT_FOLDER);
          LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache, statsbeatFolder);
          LocalFileWriter localFileWriter = new LocalFileWriter(localFileCache, statsbeatFolder);
          TelemetryChannel channel =
              TelemetryChannel.create(
                  endpointProvider.getStatsbeatEndpointUrl(), localFileWriter, null);
          LocalFileSender.start(localFileLoader, channel);
          statsbeatChannelBatcher = BatchSpanProcessor.builder(channel).build();
        }
      }
    }
    return statsbeatChannelBatcher;
  }

  /** Gets or sets the default instrumentation key for the application. */
  public String getInstrumentationKey() {
    return instrumentationKey;
  }

  /** Gets or sets the default instrumentation key for the application. */
  public void setInstrumentationKey(String key) {

    // A non null, non empty instrumentation key is a must
    if (Strings.isNullOrEmpty(key)) {
      throw new IllegalArgumentException("key");
    }

    instrumentationKey = key;
  }

  public String getStatsbeatInstrumentationKey() {
    return statsbeatInstrumentationKey;
  }

  public void setStatsbeatInstrumentationKey(String key) {
    statsbeatInstrumentationKey = key;
  }

  public @Nullable String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
    globalTags.put(ContextTagKeys.AI_CLOUD_ROLE.toString(), roleName);
  }

  public String getRoleInstance() {
    return roleInstance;
  }

  public void setRoleInstance(String roleInstance) {
    this.roleInstance = roleInstance;
    globalTags.put(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), roleInstance);
  }

  public void setConnectionString(String connectionString) {
    try {
      ConnectionString.parseInto(connectionString, this);
    } catch (InvalidConnectionStringException e) {
      throw new IllegalArgumentException("Invalid connection string", e);
    }
  }

  public EndpointProvider getEndpointProvider() {
    return endpointProvider;
  }

  public Configuration.AadAuthentication getAadAuthentication() {
    return aadAuthentication;
  }

  public void addNonFilterableMetricNames(String... metricNames) {
    nonFilterableMetricNames.addAll(asList(metricNames));
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initEventTelemetry(TelemetryItem telemetry, TelemetryEventData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, EVENT_TELEMETRY_NAME, "EventData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initExceptionTelemetry(TelemetryItem telemetry, TelemetryExceptionData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, EXCEPTION_TELEMETRY_NAME, "ExceptionData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initMessageTelemetry(TelemetryItem telemetry, MessageData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, MESSAGE_TELEMETRY_NAME, "MessageData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  // FIXME (trask) azure sdk exporter: rename MetricsData to MetricData to match the telemetryName
  //  and baseType?
  public void initMetricTelemetry(
      TelemetryItem telemetry, MetricsData data, MetricDataPoint point) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, METRIC_TELEMETRY_NAME, "MetricData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
    data.setMetrics(singletonList(point));
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initPageViewTelemetry(TelemetryItem telemetry, PageViewData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, PAGE_VIEW_TELEMETRY_NAME, "PageViewData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initRemoteDependencyTelemetry(TelemetryItem telemetry, RemoteDependencyData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, REMOTE_DEPENDENCY_TELEMETRY_NAME, "RemoteDependencyData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initRequestTelemetry(TelemetryItem telemetry, RequestData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, REQUEST_TELEMETRY_NAME, "RequestData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  private void initTelemetry(
      TelemetryItem telemetry, MonitorDomain data, String telemetryName, String baseType) {
    telemetry.setVersion(1);
    telemetry.setName(telemetryName);
    telemetry.setInstrumentationKey(instrumentationKey);
    telemetry.setTags(new HashMap<>(globalTags));

    data.setVersion(2);

    MonitorBase monitorBase = new MonitorBase();
    telemetry.setData(monitorBase);
    monitorBase.setBaseType(baseType);
    monitorBase.setBaseData(data);
  }

  // visible for testing
  public static File getTelemetryFolder(String name) {
    File subdirectory = new File(DEFAULT_FOLDER, name);

    if (!subdirectory.exists()) {
      subdirectory.mkdirs();
    }

    if (!subdirectory.exists() || !subdirectory.canRead() || !subdirectory.canWrite()) {
      throw new IllegalArgumentException(
          "subdirectory must exist and have read and write permissions.");
    }

    return subdirectory;
  }
}
