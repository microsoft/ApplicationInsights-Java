package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HeartBeatProvider implements HeartBeatProviderInterface {

  private final String HEARTBEAT_SYNTHETIC_METRIC_NAME = "HeartbeatState";

  private List<String> disableDefaultProperties = new ArrayList<>();

  private List<String> disabledHeartBeatPropertiesProviders =new ArrayList<>();

  private long heartbeatsSent;

  private ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

  private long interval;

  private TelemetryClient telemetryClient;

  private volatile boolean isEnabled;

  public HeartBeatProvider() {
    this.interval = HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL;
    this.heartbeatProperties = new ConcurrentHashMap<>();
    this.isEnabled = true;
  }

  @Override
  public String getInstrumentationKey() {
    return null;
  }

  @Override
  public void setInstrumentationKey(String key) {

  }

  @Override
  public void initialize(TelemetryConfiguration configuration) {

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
    return false;
  }

  @Override
  public void setHeartBeatEnabled(boolean isEnabled) {

  }

  @Override
  public List<String> getExcludedHeartBeatPropertyProviders() {
    return null;
  }

  @Override
  public void setExcludedHeartBeatPropertyProviders(
      List<String> excludedHeartBeatPropertyProviders) {

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
    return null;
  }

  @Override
  public void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties) {

  }
}
