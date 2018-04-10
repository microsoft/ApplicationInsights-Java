package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HeartBeatProviderMock implements HeartBeatProviderInterface {

  private ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

  private List<String> disableDefaultProperties = new ArrayList<>();

  private List<String> disabledHeartBeatPropertiesProviders = new ArrayList<>();

  private String instrumentationKey;

  private volatile boolean isEnabled;

  private long interval;

  public HeartBeatProviderMock() {
    this.heartbeatProperties = new ConcurrentHashMap<>();
    this.instrumentationKey = UUID.randomUUID().toString();
    this.isEnabled = true;
    this.interval = 31;
  }

  public ConcurrentMap<String, HeartBeatPropertyPayload> getHeartBeatProperties() {
    return this.heartbeatProperties;

  }
  @Override
  public String getInstrumentationKey() {
    return this.instrumentationKey;
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
    HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
    payload.setHealthy(isHealthy);
    payload.setPayloadValue(propertyValue);
    heartbeatProperties.put(propertyName, payload);
    return true;
  }

  @Override
  public boolean setHeartBeatProperty(String propertyName, String propertyValue,
      boolean isHealthy) {
    if (heartbeatProperties.containsKey(propertyName)) {
      HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
      payload.setHealthy(isHealthy);
      payload.setPayloadValue(propertyValue);
      heartbeatProperties.put(propertyName, payload);
      return true;
    }
    return false;
  }

  @Override
  public boolean isHeartBeatEnabled() {
    return this.isEnabled;
  }

  @Override
  public void setHeartBeatEnabled(boolean isEnabled) {

  }

  @Override
  public List<String> getExcludedHeartBeatPropertyProviders() {
    return this.disabledHeartBeatPropertiesProviders;
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

  }

  @Override
  public List<String> getExcludedHeartBeatProperties() {
    return this.disableDefaultProperties;
  }

  @Override
  public void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties) {

  }
}
