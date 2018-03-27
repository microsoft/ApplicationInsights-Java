package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HeartBeatProvider implements HeartBeatProviderInterface, Stoppable {

  private final String HEARTBEAT_SYNTHETIC_METRIC_NAME = "HeartbeatState";

  private List<String> disableDefaultProperties = new ArrayList<>();

  private List<String> disabledHeartBeatPropertiesProviders = new ArrayList<>();

  private long heartbeatsSent;

  private ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

  private long interval;

  private TelemetryClient telemetryClient;

  private ExecutorService executorService = Executors.newCachedThreadPool();

  private volatile boolean isEnabled;

  public HeartBeatProvider() {
    this.interval = HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL;
    this.heartbeatProperties = new ConcurrentHashMap<>();
    this.isEnabled = true;
  }

  @Override
  public String getInstrumentationKey() {
    return this.telemetryClient.getContext().getInstrumentationKey();
  }

  @Override
  public void setInstrumentationKey(String key) {
    if (this.telemetryClient != null && key != null) {
      this.telemetryClient.getContext().setInstrumentationKey(key);
    }
  }

  @Override
  public void initialize(TelemetryConfiguration configuration) {
    if (this.telemetryClient == null) {
      this.telemetryClient = new TelemetryClient(configuration);
    }

    //populate default payload

  }

  @Override
  public boolean addHeartBeatProperty(String propertyName, String propertyValue,
      boolean isHealthy) {
    return false;
  }

  @Override
  public boolean setHeartBeatPropertyName(String propertyName, String propertyValue,
      boolean isHealthy) {
    return false;
  }

  @Override
  public boolean isHeartBeatEnabled() {
    return isEnabled;
  }

  @Override
  public void setHeartBeatEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  @Override
  public List<String> getExcludedHeartBeatPropertyProviders() {
    return this.disabledHeartBeatPropertiesProviders;
  }

  @Override
  public void setExcludedHeartBeatPropertyProviders(
      List<String> excludedHeartBeatPropertyProviders) {
    this.disabledHeartBeatPropertiesProviders = excludedHeartBeatPropertyProviders;
  }

  @Override
  public long getHeartBeatInterval() {
    return this.interval;
  }

  @Override
  public void setHeartBeatInterval(long timeUnit) {
    // user set time unit in seconds
    if (timeUnit <= HeartBeatProviderInterface.MINIMUM_HEARTBEAT_INTERVAL) {
      this.interval = HeartBeatProviderInterface.MINIMUM_HEARTBEAT_INTERVAL;
    }
    else {
      this.interval = timeUnit;
    }
  }

  @Override
  public List<String> getExcludedHeartBeatProperties() {
    return this.disableDefaultProperties;
  }

  @Override
  public void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties) {
    this.disableDefaultProperties = excludedHeartBeatProperties;
  }

  @Override
  public void stop(long timeout, TimeUnit timeUnit) {
    executorService.shutdown();
    try {
      executorService.awaitTermination(1L, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {

    }
  }
}
