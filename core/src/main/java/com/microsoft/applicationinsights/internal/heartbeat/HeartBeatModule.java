package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HeartBeatModule implements TelemetryModule {

  private final HeartBeatProviderInterface heartBeatProviderInterface;

  public HeartBeatModule(Map<String, String> properties) {

    if (properties != null) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        switch (entry.getKey()) {
          case "HeartBeatInterval":
            try {
              setHeartBeatInterval(Long.valueOf(entry.getValue()));
            } catch (Exception e) {
              //add log
            }
          case "isHeartBeatEnabled":
            try {
              setHeartBeatEnabled(Boolean.parseBoolean(entry.getValue()));
            }
            catch (Exception e) {
              //log
            }
          case "ExcludedHeartBeatPropertiesProvider":
            try {
              List<String> excludedHeartBeatPropertiesProviderList = parseStringToList(entry.getValue());
              setExcludedHeartBeatPropertiesProvider(excludedHeartBeatPropertiesProviderList);
            }
            catch (Exception e) {
              //log
            }
          case "ExcludedHeartBeatProperties":
            try {
              List<String> excludedHeartBeatPropertiesList = parseStringToList(entry.getValue());
              setExcludedHeartBeatProperties(excludedHeartBeatPropertiesList);
            }
            catch (Exception e) {
              //log
            }
        }
      }
    }

    heartBeatProviderInterface = new HeartBeatProvider();
  }

  public long getHeartBeatInterval() {
    return this.heartBeatProviderInterface.getHeartBeatInterval();
  }

  public void setHeartBeatInterval(long heartBeatInterval) {
    this.heartBeatProviderInterface.setHeartBeatInterval(heartBeatInterval);
  }

  public List<String> getExcludedHeartBeatProperties() {
    return heartBeatProviderInterface.getExcludedHeartBeatProperties();
  }

  public void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties) {
    this.heartBeatProviderInterface.setExcludedHeartBeatProperties(excludedHeartBeatProperties);
  }

  public List<String> getExcludedHeartBeatPropertiesProvider() {
    return heartBeatProviderInterface.getExcludedHeartBeatPropertyProviders();
  }

  public void setExcludedHeartBeatPropertiesProvider(
      List<String> excludedHeartBeatPropertiesProvider) {
      this.heartBeatProviderInterface.setExcludedHeartBeatPropertyProviders(excludedHeartBeatPropertiesProvider);
  }

  public boolean isHeartBeatEnabled() {
    return this.heartBeatProviderInterface.isHeartBeatEnabled();
  }

  public void setHeartBeatEnabled(boolean heartBeatEnabled) {
    this.heartBeatProviderInterface.setHeartBeatEnabled(heartBeatEnabled);
  }

  @Override
  public void initialize(TelemetryConfiguration configuration) {
    InternalLogger.INSTANCE.info("heartbeat is enabled");
  }

  private List<String> parseStringToList(String value) {
    if (value == null || value.length() == 0) return new ArrayList<>();
    List<String> valueList = Arrays.asList(value.split(";"));
    return valueList;
  }

}
