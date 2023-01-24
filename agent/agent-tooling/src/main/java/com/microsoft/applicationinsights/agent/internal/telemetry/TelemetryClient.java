// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import com.azure.core.http.HttpPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.AvailabilityTelemetryBuilder;
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
import com.azure.monitor.opentelemetry.exporter.implementation.logging.DiagnosticTelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.statsbeat.NetworkStatsbeatHttpPipelinePolicy;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatTelemetryPipelineListener;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;

public class TelemetryClient {

  private static final String TELEMETRY_FOLDER_NAME = "telemetry";
  private static final String STATSBEAT_FOLDER_NAME = "statsbeat";

  @Nullable private static volatile TelemetryClient active;

  private final AppIdSupplier appIdSupplier;

  @Nullable private volatile ConnectionString connectionString;
  @Nullable private volatile StatsbeatConnectionString statsbeatConnectionString;
  @Nullable private volatile String roleName;
  @Nullable private volatile String roleInstance;

  // globalTags contain:
  // * cloud role name
  // * cloud role instance
  // * sdk version
  // * application version (if provided in customDimensions)
  private final Map<String, String> globalTags;
  // contains customDimensions from json configuration
  private final Map<String, String> globalProperties;

  private final List<MetricFilter> metricFilters;

  @Nullable private volatile QuickPulse quickPulse;

  private final StatsbeatModule statsbeatModule;
  @Nullable private final File tempDir;
  private final int generalExportQueueCapacity;
  private final int metricsExportQueueCapacity;
  private final int diskPersistenceMaxSizeMb;

  @Nullable private final Configuration.AadAuthentication aadAuthentication;

  private final Object batchItemProcessorInitLock = new Object();
  @Nullable private volatile BatchItemProcessor generalBatchItemProcessor;
  @Nullable private volatile BatchItemProcessor metricsBatchItemProcessor;
  @Nullable private volatile BatchItemProcessor statsbeatBatchItemProcessor;

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
    this.connectionString = builder.connectionString;
    this.statsbeatConnectionString = builder.statsbeatConnectionString;
    this.roleName = builder.roleName;
    this.roleInstance = builder.roleInstance;
    this.diskPersistenceMaxSizeMb = builder.diskPersistenceMaxSizeMb;

    appIdSupplier = new AppIdSupplier();
    if (this.connectionString != null) {
      appIdSupplier.updateAppId(this.connectionString);
    }
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
      if (metricsData.getMetrics().isEmpty()) {
        throw new AssertionError("MetricsData has no metric point");
      }
      MetricDataPoint point = metricsData.getMetrics().get(0);
      String metricName = point.getName();
      if (MetricFilter.shouldSkip(metricName, metricFilters)) {
        return;
      }

      if (!Double.isFinite(point.getValue())) {
        // TODO (trask) add test for this
        // breeze doesn't like these values
        return;
      }
    }

    if (telemetryItem.getTime() == null) {
      // this is easy to forget when adding new telemetry
      throw new AssertionError("telemetry item is missing time");
    }

    if (quickPulse != null) {
      quickPulse.add(telemetryItem);
    }

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

  public BatchItemProcessor getGeneralBatchItemProcessor() {
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
  public BatchItemProcessor getMetricsBatchItemProcessor() {
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
    TelemetryPipeline telemetryPipeline = new TelemetryPipeline(httpPipeline);

    TelemetryPipelineListener telemetryPipelineListener;
    if (tempDir == null) {
      telemetryPipelineListener =
          new DiagnosticTelemetryPipelineListener(
              "Sending telemetry to the ingestion service", false);
    } else {
      telemetryPipelineListener =
          TelemetryPipelineListener.composite(
              // suppress warnings on retryable failures, in order to reduce sporadic/annoying
              // warnings when storing to disk and retrying shortly afterwards anyways
              // will log if that retry from disk fails
              new DiagnosticTelemetryPipelineListener(
                  "Sending telemetry to the ingestion service", true),
              new LocalStorageTelemetryPipelineListener(
                  diskPersistenceMaxSizeMb,
                  TempDirs.getSubDir(tempDir, TELEMETRY_FOLDER_NAME),
                  telemetryPipeline,
                  statsbeatModule.getNonessentialStatsbeat(),
                  false));
    }

    return BatchItemProcessor.builder(
            new TelemetryItemExporter(telemetryPipeline, telemetryPipelineListener))
        .setMaxQueueSize(exportQueueCapacity)
        .setMaxExportBatchSize(maxExportBatchSize)
        // the number 100 was calculated as the max number of concurrent exports that the single
        // worker thread can drive, so anything higher than this should not increase throughput
        .setMaxPendingExports(100)
        .build(queueName);
  }

  public BatchItemProcessor getStatsbeatBatchItemProcessor() {
    if (statsbeatBatchItemProcessor == null) {
      synchronized (batchItemProcessorInitLock) {
        if (statsbeatBatchItemProcessor == null) {
          HttpPipeline httpPipeline = LazyHttpClient.newHttpPipeLine(null);
          TelemetryPipeline telemetryPipeline = new TelemetryPipeline(httpPipeline);

          TelemetryPipelineListener telemetryPipelineListener;
          if (tempDir == null) {
            telemetryPipelineListener =
                new StatsbeatTelemetryPipelineListener(statsbeatModule::shutdown);
          } else {
            LocalStorageTelemetryPipelineListener localStorageTelemetryPipelineListener =
                new LocalStorageTelemetryPipelineListener(
                    diskPersistenceMaxSizeMb,
                    TempDirs.getSubDir(tempDir, STATSBEAT_FOLDER_NAME),
                    telemetryPipeline,
                    LocalStorageStats.noop(),
                    true);
            telemetryPipelineListener =
                TelemetryPipelineListener.composite(
                    new StatsbeatTelemetryPipelineListener(
                        () -> {
                          statsbeatModule.shutdown();
                          localStorageTelemetryPipelineListener.shutdown();
                        }),
                    localStorageTelemetryPipelineListener);
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
  public StatsbeatConnectionString getStatsbeatConnectionString() {
    return statsbeatConnectionString;
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

  public AvailabilityTelemetryBuilder newAvailabilityTelemetryBuilder() {
    return newTelemetryBuilder(AvailabilityTelemetryBuilder::create);
  }

  private <T extends AbstractTelemetryBuilder> T newTelemetryBuilder(Supplier<T> creator) {
    T telemetry = creator.get();
    populateDefaults(telemetry);
    return telemetry;
  }

  public void populateDefaults(AbstractTelemetryBuilder telemetryBuilder, Resource resource) {
    // the agent does not currently factor the resource attributes into the cloud role
    populateDefaults(telemetryBuilder);
  }

  private void populateDefaults(AbstractTelemetryBuilder telemetryBuilder) {
    if (connectionString != null) {
      // not sure if connectionString can be null in Azure Functions
      telemetryBuilder.setConnectionString(connectionString);
    }
    for (Map.Entry<String, String> entry : globalTags.entrySet()) {
      telemetryBuilder.addTag(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }
  }

  @Nullable
  public ConnectionString getConnectionString() {
    return connectionString;
  }

  @Nullable
  public String getRoleName() {
    return roleName;
  }

  @Nullable
  public String getRoleInstance() {
    return roleInstance;
  }

  // used during Azure Functions placeholder specialization
  // and also used by Azure Spring Apps dynamic configuration
  public void updateConnectionStrings(
      @Nullable String connectionString,
      @Nullable String statsbeatInstrumentationKey,
      @Nullable String statsbeatEndpoint) {

    if (Strings.isNullOrEmpty(connectionString)) {
      this.connectionString = null;
      appIdSupplier.updateAppId(null);
      this.statsbeatConnectionString = null;
      return;
    }

    this.connectionString = ConnectionString.parse(connectionString);
    appIdSupplier.updateAppId(this.connectionString);

    this.statsbeatConnectionString =
        StatsbeatConnectionString.create(
            this.connectionString, statsbeatInstrumentationKey, statsbeatEndpoint);
    if (this.statsbeatConnectionString == null) {
      statsbeatModule.shutdown();
    }
  }

  public void updateRoleName(String roleName) {
    this.roleName = roleName;
    globalTags.put(ContextTagKeys.AI_CLOUD_ROLE.toString(), roleName);
  }

  public void updateRoleInstance(String roleInstance) {
    this.roleInstance = roleInstance;
    globalTags.put(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), roleInstance);
  }

  public String getAppId() {
    return appIdSupplier.get();
  }

  @Nullable
  public Configuration.AadAuthentication getAadAuthentication() {
    return aadAuthentication;
  }

  public StatsbeatModule getStatsbeatModule() {
    return statsbeatModule;
  }

  public void setQuickPulse(@Nullable QuickPulse quickPulse) {
    this.quickPulse = quickPulse;
  }

  public static class Builder {

    private Map<String, String> globalTags;
    private Map<String, String> globalProperties;
    private List<MetricFilter> metricFilters;
    private StatsbeatModule statsbeatModule;
    @Nullable private File tempDir;
    private int generalExportQueueCapacity;
    private int metricsExportQueueCapacity;
    @Nullable private Configuration.AadAuthentication aadAuthentication;
    @Nullable private ConnectionString connectionString;
    @Nullable private StatsbeatConnectionString statsbeatConnectionString;
    @Nullable private String roleName;
    @Nullable private String roleInstance;
    private int diskPersistenceMaxSizeMb;

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
      this.aadAuthentication = aadAuthentication;
      return this;
    }

    public Builder setConnectionStrings(
        @Nullable String connectionString,
        @Nullable String statsbeatInstrumentationKey,
        @Nullable String statsbeatEndpoint) {

      if (Strings.isNullOrEmpty(connectionString)) {
        this.connectionString = null;
        this.statsbeatConnectionString = null;
      } else {
        this.connectionString = ConnectionString.parse(connectionString);
        this.statsbeatConnectionString =
            StatsbeatConnectionString.create(
                this.connectionString, statsbeatInstrumentationKey, statsbeatEndpoint);
        if (this.statsbeatConnectionString == null) {
          statsbeatModule.shutdown();
        }
      }
      return this;
    }

    public Builder setRoleName(@Nullable String roleName) {
      this.roleName = roleName;
      globalTags.put(ContextTagKeys.AI_CLOUD_ROLE.toString(), roleName);
      return this;
    }

    public Builder setRoleInstance(@Nullable String roleInstance) {
      this.roleInstance = roleInstance;
      globalTags.put(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), roleInstance);
      return this;
    }

    public Builder setDiskPersistenceMaxSizeMb(int diskPersistenceMaxSizeMb) {
      this.diskPersistenceMaxSizeMb = diskPersistenceMaxSizeMb;
      return this;
    }

    public TelemetryClient build() {
      return new TelemetryClient(this);
    }
  }
}
