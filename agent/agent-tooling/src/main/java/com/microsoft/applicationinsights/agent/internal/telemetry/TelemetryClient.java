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

import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AuthenticationType.CLIENTSECRET;
import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AuthenticationType.SAMI;
import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AuthenticationType.UAMI;
import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AuthenticationType.VSCODE;
import static java.util.Arrays.asList;

import com.azure.core.http.HttpPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.EventTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.ExceptionTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.PageViewTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.RemoteDependencyTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.RequestTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.StatsbeatConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.localstorage.LocalStorageStats;
import com.azure.monitor.opentelemetry.exporter.implementation.localstorage.LocalStorageTelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulseDataCollector;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.AadAuthentication;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.AadAuthenticationBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.AuthenticationType;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.LazyHttpClient;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.statsbeat.NetworkStatsbeatHttpPipelinePolicy;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
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

  private static final String TELEMETRY_FOLDER_NAME = "telemetry";
  private static final String STATSBEAT_FOLDER_NAME = "statsbeat";

  private static volatile @MonotonicNonNull TelemetryClient active;

  private final Set<String> nonFilterableMetricNames = new HashSet<>();

  @Nullable private volatile ConnectionString connectionString;
  @Nullable private volatile StatsbeatConnectionString statsbeatConnectionString;
  private volatile @MonotonicNonNull String roleName;
  private volatile @MonotonicNonNull String roleInstance;

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
  @Nullable private final File tempDir;
  private final int generalExportQueueCapacity;
  private final int metricsExportQueueCapacity;

  @Nullable private final AadAuthentication aadAuthentication;

  private final Object batchItemProcessorInitLock = new Object();
  private volatile @MonotonicNonNull BatchItemProcessor generalBatchItemProcessor;
  private volatile @MonotonicNonNull BatchItemProcessor metricsBatchItemProcessor;
  private volatile @MonotonicNonNull BatchItemProcessor statsbeatBatchItemProcessor;

  public static TelemetryClient.Builder builder() {
    return new TelemetryClient.Builder();
  }

  // TODO (trask) reduce usage of this
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
    this.tempDir = builder.tempDir;
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
    if (connectionString == null) {
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
      getMetricsBatchItemProcessor().trackAsync(telemetryItem);
    } else {
      getGeneralBatchItemProcessor().trackAsync(telemetryItem);
    }
  }

  public void trackStatsbeatAsync(TelemetryItem telemetry) {
    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    getStatsbeatBatchItemProcessor().trackAsync(telemetry);
  }

  public CompletableResultCode forceFlush() {
    List<CompletableResultCode> resultCodes = new ArrayList<>();
    if (generalBatchItemProcessor != null) {
      resultCodes.add(generalBatchItemProcessor.forceFlush());
    }
    if (metricsBatchItemProcessor != null) {
      resultCodes.add(metricsBatchItemProcessor.forceFlush());
    }
    if (statsbeatBatchItemProcessor != null) {
      resultCodes.add(statsbeatBatchItemProcessor.forceFlush());
    }
    return CompletableResultCode.ofAll(resultCodes);
  }

  private BatchItemProcessor getGeneralBatchItemProcessor() {
    if (generalBatchItemProcessor == null) {
      synchronized (batchItemProcessorInitLock) {
        if (generalBatchItemProcessor == null) {
          generalBatchItemProcessor =
              initBatchItemProcessor(generalExportQueueCapacity, 512, "general");
        }
      }
    }
    return generalBatchItemProcessor;
  }

  // metrics get flooded every 60 seconds by default, so need much larger queue size to avoid
  // dropping telemetry (they are much smaller so a larger queue size and larger batch size are ok)
  private BatchItemProcessor getMetricsBatchItemProcessor() {
    if (metricsBatchItemProcessor == null) {
      synchronized (batchItemProcessorInitLock) {
        if (metricsBatchItemProcessor == null) {
          metricsBatchItemProcessor =
              initBatchItemProcessor(metricsExportQueueCapacity, 2048, "metrics");
        }
      }
    }
    return metricsBatchItemProcessor;
  }

  private BatchItemProcessor initBatchItemProcessor(
      int exportQueueCapacity, int maxExportBatchSize, String queueName) {

    HttpPipeline httpPipeline =
        LazyHttpClient.newHttpPipeLine(
            aadAuthentication,
            new NetworkStatsbeatHttpPipelinePolicy(statsbeatModule.getNetworkStatsbeat()));
    TelemetryPipeline telemetryPipeline =
        new TelemetryPipeline(httpPipeline, connectionString.getIngestionEndpoint());

    TelemetryPipelineListener telemetryPipelineListener;
    if (tempDir == null) {
      telemetryPipelineListener = TelemetryPipelineListener.noop();
    } else {
      telemetryPipelineListener =
          new LocalStorageTelemetryPipelineListener(
              TempDirs.getSubDir(tempDir, TELEMETRY_FOLDER_NAME),
              telemetryPipeline,
              statsbeatModule.getNonessentialStatsbeat());
    }

    return BatchItemProcessor.builder(
            new TelemetryItemExporter(telemetryPipeline, telemetryPipelineListener))
        .setMaxQueueSize(exportQueueCapacity)
        .setMaxExportBatchSize(maxExportBatchSize)
        .build(queueName);
  }

  public BatchItemProcessor getStatsbeatBatchItemProcessor() {
    if (statsbeatBatchItemProcessor == null) {
      synchronized (batchItemProcessorInitLock) {
        if (statsbeatBatchItemProcessor == null) {
          HttpPipeline httpPipeline = LazyHttpClient.newHttpPipeLine(null);
          TelemetryPipeline telemetryPipeline =
              new TelemetryPipeline(httpPipeline, statsbeatConnectionString.getEndpoint());

          TelemetryPipelineListener telemetryPipelineListener;
          if (tempDir == null) {
            telemetryPipelineListener = TelemetryPipelineListener.noop();
          } else {
            telemetryPipelineListener =
                new LocalStorageTelemetryPipelineListener(
                    TempDirs.getSubDir(tempDir, STATSBEAT_FOLDER_NAME),
                    telemetryPipeline,
                    LocalStorageStats.noop());
          }

          TelemetryItemExporter exporter =
              new TelemetryItemExporter(telemetryPipeline, telemetryPipelineListener);

          statsbeatBatchItemProcessor = BatchItemProcessor.builder(exporter).build("statsbeat");
        }
      }
    }
    return statsbeatBatchItemProcessor;
  }

  /** Gets or sets the default instrumentation key for the application. */
  @Nullable
  public String getInstrumentationKey() {
    ConnectionString val = this.connectionString;
    return val != null ? val.getInstrumentationKey() : null;
  }

  @Nullable
  public String getStatsbeatInstrumentationKey() {
    StatsbeatConnectionString val = this.statsbeatConnectionString;
    return val != null ? val.getInstrumentationKey() : null;
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
    telemetryBuilder.setInstrumentationKey(getInstrumentationKey());
    for (Map.Entry<String, String> entry : globalTags.entrySet()) {
      telemetryBuilder.addTag(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }
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

  public void setConnectionString(ConnectionString connectionString) {
    this.connectionString = connectionString;
  }

  public void setStatsbeatConnectionString(StatsbeatConnectionString statsbeatConnectionString) {
    this.statsbeatConnectionString = statsbeatConnectionString;
  }

  @Nullable
  public ConnectionString getConnectionString() {
    return connectionString;
  }

  @Nullable
  public AadAuthentication getAadAuthentication() {
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
    @Nullable private File tempDir;
    private int generalExportQueueCapacity;
    private int metricsExportQueueCapacity;
    @Nullable private AadAuthentication aadAuthentication;

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

    public Builder setTempDir(@Nullable File tempDir) {
      this.tempDir = tempDir;
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
      this.aadAuthentication = aadAuthenticationMapper(aadAuthentication);
      return this;
    }

    public TelemetryClient build() {
      return new TelemetryClient(this);
    }

    @Nullable
    private static AadAuthentication aadAuthenticationMapper(
        Configuration.AadAuthentication aadAuthentication) {
      if (!aadAuthentication.enabled) {
        return null;
      }
      return new AadAuthenticationBuilder(aadAuthenticationTypeMapper(aadAuthentication.type))
          .clientId(aadAuthentication.clientId)
          .clientSecret(aadAuthentication.clientSecret)
          .authorityHost(aadAuthentication.authorityHost)
          .tenantId(aadAuthentication.tenantId)
          .build();
    }

    private static AuthenticationType aadAuthenticationTypeMapper(
        Configuration.AuthenticationType authenticationType) {
      switch (authenticationType) {
        case UAMI:
          return UAMI;
        case SAMI:
          return SAMI;
        case CLIENTSECRET:
          return CLIENTSECRET;
        case VSCODE:
          return VSCODE;
        default:
          throw new IllegalStateException(
              "AAD Authentication configuration of type: " + authenticationType + " is invalid");
      }
    }
  }
}
