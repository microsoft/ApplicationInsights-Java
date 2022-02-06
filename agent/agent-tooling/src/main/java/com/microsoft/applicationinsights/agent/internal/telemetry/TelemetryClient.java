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

import com.azure.core.http.HttpPipeline;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.AbstractTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.EventTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.ExceptionTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.MessageTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.MetricTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.PageViewTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.RemoteDependencyTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.builders.RequestTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.models.ContextTagKeys;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricDataPoint;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorDomain;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileCache;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileLoader;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileSender;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalStorageTelemetryPipelineListener;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalStorageUtils;
import com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseDataCollector;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatHttpPipelinePolicy;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class TelemetryClient {

  private static volatile @MonotonicNonNull TelemetryClient active;

  private final Set<String> nonFilterableMetricNames = new HashSet<>();

  @Nullable private volatile String instrumentationKey;
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

  private final StatsbeatModule statsbeatModule;
  private final boolean readOnlyFileSystem;
  private final int generalExportQueueCapacity;
  private final int metricsExportQueueCapacity;

  @Nullable private final Configuration.AadAuthentication aadAuthentication;

  private final Object channelInitLock = new Object();
  private volatile @MonotonicNonNull BatchSpanProcessor generalChannelBatcher;
  private volatile @MonotonicNonNull BatchSpanProcessor metricsChannelBatcher;
  private volatile @MonotonicNonNull BatchSpanProcessor statsbeatChannelBatcher;

  public static TelemetryClient.Builder builder() {
    return new TelemetryClient.Builder();
  }

  // only used by tests
  public static TelemetryClient createForTest() {
    return builder()
        .setCustomDimensions(new HashMap<>())
        .setMetricFilters(new ArrayList<>())
        .setStatsbeatModule(new StatsbeatModule())
        .build();
  }

  public TelemetryClient(Builder builder) {
    this.globalTags = builder.globalTags;
    this.globalProperties = builder.globalProperties;
    this.metricFilters = builder.metricFilters;
    this.statsbeatModule = builder.statsbeatModule;
    this.readOnlyFileSystem = builder.readOnlyFileSystem;
    this.generalExportQueueCapacity = builder.generalExportQueueCapacity;
    this.metricsExportQueueCapacity = builder.metricsExportQueueCapacity;
    this.aadAuthentication = builder.aadAuthentication;
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

  public void trackAsync(TelemetryItem telemetryItem) {
    if (Strings.isNullOrEmpty(instrumentationKey)) {
      return;
    }

    MonitorDomain data = telemetryItem.getData().getBaseData();

    if (data instanceof MetricsData) {
      MetricsData metricsData = (MetricsData) data;
      // TODO (trask) there can only be a single point so this is excessive
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

    if (telemetryItem.getTime() == null) {
      // this is easy to forget when adding new telemetry
      throw new AssertionError("telemetry item is missing time");
    }

    QuickPulseDataCollector.INSTANCE.add(telemetryItem);

    TelemetryObservers.INSTANCE.getObservers().forEach(consumer -> consumer.accept(telemetryItem));

    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    if (data instanceof MetricsData) {
      getMetricsChannelBatcher().trackAsync(telemetryItem);
    } else {
      getGeneralChannelBatcher().trackAsync(telemetryItem);
    }
  }

  public void trackStatsbeatAsync(TelemetryItem telemetry) {
    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    getStatsbeatChannelBatcher().trackAsync(telemetry);
  }

  public CompletableResultCode flushChannelBatcher() {
    if (generalChannelBatcher != null) {
      return generalChannelBatcher.forceFlush();
    } else {
      return CompletableResultCode.ofSuccess();
    }
  }

  private BatchSpanProcessor getGeneralChannelBatcher() {
    if (generalChannelBatcher == null) {
      synchronized (channelInitLock) {
        if (generalChannelBatcher == null) {
          generalChannelBatcher = initChannelBatcher(generalExportQueueCapacity, 512, "general");
        }
      }
    }
    return generalChannelBatcher;
  }

  // metrics get flooded every 60 seconds by default, so need much larger queue size to avoid
  // dropping telemetry (they are much smaller so a larger queue size and larger batch size are ok)
  private BatchSpanProcessor getMetricsChannelBatcher() {
    if (metricsChannelBatcher == null) {
      synchronized (channelInitLock) {
        if (metricsChannelBatcher == null) {
          metricsChannelBatcher = initChannelBatcher(metricsExportQueueCapacity, 2048, "metrics");
        }
      }
    }
    return metricsChannelBatcher;
  }

  private BatchSpanProcessor initChannelBatcher(
      int exportQueueCapacity, int maxExportBatchSize, String queueName) {
    LocalFileLoader localFileLoader = null;
    LocalFileWriter localFileWriter = null;
    TelemetryPipelineListener telemetryPipelineListener = TelemetryPipelineListener.noop();
    if (!readOnlyFileSystem) {
      File telemetryFolder = LocalStorageUtils.getOfflineTelemetryFolder();
      LocalFileCache localFileCache = new LocalFileCache(telemetryFolder);
      localFileLoader =
          new LocalFileLoader(
              localFileCache, telemetryFolder, statsbeatModule.getNonessentialStatsbeat());
      localFileWriter =
          new LocalFileWriter(
              localFileCache, telemetryFolder, statsbeatModule.getNonessentialStatsbeat());
      telemetryPipelineListener = new LocalStorageTelemetryPipelineListener(localFileWriter);
    }

    HttpPipeline httpPipeline =
        LazyHttpClient.newHttpPipeLine(
            aadAuthentication,
            true,
            new StatsbeatHttpPipelinePolicy(statsbeatModule.getNetworkStatsbeat()));
    TelemetryPipeline telemetryPipeline =
        new TelemetryPipeline(httpPipeline, endpointProvider.getIngestionEndpointUrl());

    TelemetryItemPipeline telemetryItemPipeline =
        new TelemetryItemPipeline(telemetryPipeline, telemetryPipelineListener);

    if (!readOnlyFileSystem) {
      LocalFileSender.start(localFileLoader, telemetryPipeline);
    }

    return BatchSpanProcessor.builder(telemetryItemPipeline)
        .setMaxQueueSize(exportQueueCapacity)
        .setMaxExportBatchSize(maxExportBatchSize)
        .build(queueName);
  }

  public BatchSpanProcessor getStatsbeatChannelBatcher() {
    if (statsbeatChannelBatcher == null) {
      synchronized (channelInitLock) {
        if (statsbeatChannelBatcher == null) {
          File statsbeatFolder;
          LocalFileLoader localFileLoader = null;
          LocalFileWriter localFileWriter = null;
          TelemetryPipelineListener telemetryPipelineListener = TelemetryPipelineListener.noop();
          if (!readOnlyFileSystem) {
            statsbeatFolder = LocalStorageUtils.getOfflineStatsbeatFolder();
            LocalFileCache localFileCache = new LocalFileCache(statsbeatFolder);
            localFileLoader = new LocalFileLoader(localFileCache, statsbeatFolder, null);
            localFileWriter = new LocalFileWriter(localFileCache, statsbeatFolder, null);
            telemetryPipelineListener = new LocalStorageTelemetryPipelineListener(localFileWriter);
          }

          HttpPipeline httpPipeline = LazyHttpClient.newHttpPipeLine(null, true, null);
          TelemetryPipeline telemetryPipeline =
              new TelemetryPipeline(httpPipeline, endpointProvider.getStatsbeatEndpointUrl());

          TelemetryItemPipeline telemetryItemPipeline =
              new TelemetryItemPipeline(telemetryPipeline, telemetryPipelineListener);

          if (!readOnlyFileSystem) {
            LocalFileSender.start(localFileLoader, telemetryPipeline);
          }

          statsbeatChannelBatcher =
              BatchSpanProcessor.builder(telemetryItemPipeline).build("statsbeat");
        }
      }
    }
    return statsbeatChannelBatcher;
  }

  /** Gets or sets the default instrumentation key for the application. */
  public String getInstrumentationKey() {
    return instrumentationKey;
  }

  // convenience
  public TelemetryItem newMetricTelemetry(String name, double value) {
    return newMetricTelemetryBuilder(name, value).build();
  }

  public EventTelemetryBuilder newEventTelemetryBuilder() {
    return newTelemetryBuilder(EventTelemetryBuilder::create);
  }

  public ExceptionTelemetryBuilder newExceptionTelemetryBuilder() {
    return newTelemetryBuilder(ExceptionTelemetryBuilder::create);
  }

  public MessageTelemetryBuilder newMessageTelemetryBuilder() {
    return newTelemetryBuilder(MessageTelemetryBuilder::create);
  }

  // this does not populate the time
  public MetricTelemetryBuilder newMetricTelemetryBuilder() {
    return newTelemetryBuilder(MetricTelemetryBuilder::create);
  }

  // this _does_ populate the current time
  public MetricTelemetryBuilder newMetricTelemetryBuilder(String name, double value) {
    return newTelemetryBuilder(() -> MetricTelemetryBuilder.create(name, value));
  }

  public PageViewTelemetryBuilder newPageViewTelemetryBuilder() {
    return newTelemetryBuilder(PageViewTelemetryBuilder::create);
  }

  public RemoteDependencyTelemetryBuilder newRemoteDependencyTelemetryBuilder() {
    return newTelemetryBuilder(RemoteDependencyTelemetryBuilder::create);
  }

  public RequestTelemetryBuilder newRequestTelemetryBuilder() {
    return newTelemetryBuilder(RequestTelemetryBuilder::create);
  }

  private <T extends AbstractTelemetryBuilder> T newTelemetryBuilder(Supplier<T> creator) {
    T telemetry = creator.get();
    populateDefaults(telemetry);
    return telemetry;
  }

  private void populateDefaults(AbstractTelemetryBuilder telemetryBuilder) {
    telemetryBuilder.setInstrumentationKey(instrumentationKey);
    for (Map.Entry<String, String> entry : globalTags.entrySet()) {
      telemetryBuilder.addTag(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }
  }

  /** Gets or sets the default instrumentation key for the application. */
  public void setInstrumentationKey(@Nullable String key) {
    instrumentationKey = key;
  }

  public String getStatsbeatInstrumentationKey() {
    return statsbeatInstrumentationKey;
  }

  public void setStatsbeatInstrumentationKey(String key) {
    statsbeatInstrumentationKey = key;
  }

  @Nullable
  public String getRoleName() {
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
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Invalid endpoint urls.", e);
    }
  }

  public EndpointProvider getEndpointProvider() {
    return endpointProvider;
  }

  public Configuration.AadAuthentication getAadAuthentication() {
    return aadAuthentication;
  }

  public StatsbeatModule getStatsbeatModule() {
    return statsbeatModule;
  }

  public void addNonFilterableMetricNames(String... metricNames) {
    nonFilterableMetricNames.addAll(asList(metricNames));
  }

  public static class Builder {

    private Map<String, String> globalTags;
    private Map<String, String> globalProperties;
    private List<MetricFilter> metricFilters;
    private StatsbeatModule statsbeatModule;
    private boolean readOnlyFileSystem;
    private int generalExportQueueCapacity;
    private int metricsExportQueueCapacity;
    @Nullable private Configuration.AadAuthentication aadAuthentication;

    public Builder setCustomDimensions(Map<String, String> customDimensions) {
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

      return this;
    }

    public Builder setMetricFilters(List<MetricFilter> metricFilters) {
      this.metricFilters = metricFilters;
      return this;
    }

    public Builder setStatsbeatModule(StatsbeatModule statsbeatModule) {
      this.statsbeatModule = statsbeatModule;
      return this;
    }

    public Builder setReadOnlyFileSystem(boolean readOnlyFileSystem) {
      this.readOnlyFileSystem = readOnlyFileSystem;
      return this;
    }

    public Builder setGeneralExportQueueSize(int generalExportQueueCapacity) {
      this.generalExportQueueCapacity = generalExportQueueCapacity;
      return this;
    }

    public Builder setMetricsExportQueueSize(int metricsExportQueueCapacity) {
      this.metricsExportQueueCapacity = metricsExportQueueCapacity;
      return this;
    }

    public Builder setAadAuthentication(Configuration.AadAuthentication aadAuthentication) {
      this.aadAuthentication = aadAuthentication;
      return this;
    }

    public TelemetryClient build() {
      return new TelemetryClient(this);
    }
  }
}
