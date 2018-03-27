package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface HeartBeatProviderInterface {

  long DEFAULT_HEARTBEAT_INTERVAL = TimeUnit.MINUTES.toSeconds(15);

  long MINIMUM_HEARTBEAT_INTERVAL = TimeUnit.SECONDS.toSeconds(30);

  String getInstrumentationKey();

  void setInstrumentationKey(String key);

  void initialize(TelemetryConfiguration configuration);

  boolean addHeartBeatProperty(String propertyName, String propertyValue, boolean isHealthy);

  boolean setHeartBeatPropertyName(String propertyName, String propertyValue, boolean isHealthy);

  boolean isHeartBeatEnabled();

  void setHeartBeatEnabled(boolean isEnabled);

  List<String> getExcludedHeartBeatPropertyProviders();

  void setExcludedHeartBeatPropertyProviders(List<String> excludedHeartBeatPropertyProviders);

  long getHeartBeatInterval();

  void setHeartBeatInterval(long timeUnit);

  List<String> getExcludedHeartBeatProperties();

  void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties);

}
