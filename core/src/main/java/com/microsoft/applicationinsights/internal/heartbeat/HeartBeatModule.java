package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <h1>HeartBeat Provider Module</h1>
 *
 * <p>
 *   This module is the core module which is is configured by default with default settings
 *   when ApplicationInsights SDK boots up. It is used to transmit diagnostic heartbeats to
 *   Application Insights backend.
 * </p>
 *
 * @author Dhaval Doshi
 */
public class HeartBeatModule implements TelemetryModule {

  /**
   * Interface object holding concrete implementation of heartbeat provider.
   */
  private final HeartBeatProviderInterface heartBeatProviderInterface;

  private final Object lock = new Object();

  /**
   * Flag to seek if module is initialized
   */
  private static boolean isInitialized = false;

  /**
   * Default constructor to initialize the default heartbeat configuration.
   */
  public HeartBeatModule() {
    heartBeatProviderInterface = new HeartBeatProvider();
  }

  /**
   * Initializes the heartbeat configuration based on connfiguration properties specified in
   * ApplicationInsights.xml file.
   *
   * @param properties Map of properties
   */
  public HeartBeatModule(Map<String, String> properties) {

    heartBeatProviderInterface = new HeartBeatProvider();

    if (properties != null) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        switch (entry.getKey()) {
          case "HeartBeatInterval":
            try {
              setHeartBeatInterval(Long.valueOf(entry.getValue()));
              break;
            } catch (Exception e) {
              InternalLogger.INSTANCE.trace("Exception while adding Heartbeat interval,"
                  + " Stack trace is: %s", ExceptionUtils.getStackTrace(e));
            }
          case "isHeartBeatEnabled":
            try {
              setHeartBeatEnabled(Boolean.parseBoolean(entry.getValue()));
              break;
            }
            catch (Exception e) {
              InternalLogger.INSTANCE.trace("Exception while adding enabling/disabling heartbeat,"
                  + " Stack trace is: %s", ExceptionUtils.getStackTrace(e));
            }
          case "ExcludedHeartBeatPropertiesProvider":
            try {
              List<String> excludedHeartBeatPropertiesProviderList = parseStringToList(entry.getValue());
              setExcludedHeartBeatPropertiesProvider(excludedHeartBeatPropertiesProviderList);
              break;
            }
            catch (Exception e) {
              InternalLogger.INSTANCE.trace("Exception while adding Excluded Heartbeat providers,"
                  + " Stack trace is: %s", ExceptionUtils.getStackTrace(e));
            }
          case "ExcludedHeartBeatProperties":
            try {
              List<String> excludedHeartBeatPropertiesList = parseStringToList(entry.getValue());
              setExcludedHeartBeatProperties(excludedHeartBeatPropertiesList);
              break;
            }
            catch (Exception e) {
              InternalLogger.INSTANCE.trace("Exception while adding excluded heartbeat properties,"
                  + " Stack trace is: %s", ExceptionUtils.getStackTrace(e));
            }
          default:
            InternalLogger.INSTANCE.trace("Encountered unknown parameter, no action will be performed");
            break;
        }
      }
    }

  }

  /**
   * Returns the heartbeat interval in seconds.
   * @return heartbeat interval in seconds.
   */
  public long getHeartBeatInterval() {
    return this.heartBeatProviderInterface.getHeartBeatInterval();
  }

  /**
   * Sets the heartbeat interval in seconds.
   * @param heartBeatInterval Heartbeat interval to set in seconds.
   */
  public void setHeartBeatInterval(long heartBeatInterval) {
    this.heartBeatProviderInterface.setHeartBeatInterval(heartBeatInterval);
  }

  /**
   * Returns list of excluded heartbeat properties from payload
   * @return list of excluded heartbeat properties.
   */
  public List<String> getExcludedHeartBeatProperties() {
    return heartBeatProviderInterface.getExcludedHeartBeatProperties();
  }

  /**
   * Sets the list of excluded heartbeat properties
   * @param excludedHeartBeatProperties List of heartbeat properties to exclude
   */
  public void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties) {
    this.heartBeatProviderInterface.setExcludedHeartBeatProperties(excludedHeartBeatProperties);
  }

  /**
   * Gets list of excluded heartbeat properties provider.
   * @return list of excluded heartbeat properties provider.
   */
  public List<String> getExcludedHeartBeatPropertiesProvider() {
    return heartBeatProviderInterface.getExcludedHeartBeatPropertyProviders();
  }

  /**
   * Sets list of excluded heartbeat properties provider.
   * @param excludedHeartBeatPropertiesProvider list of excluded heartbeat properties provider to be excluded.
   */
  public void setExcludedHeartBeatPropertiesProvider(
      List<String> excludedHeartBeatPropertiesProvider) {
      this.heartBeatProviderInterface.setExcludedHeartBeatPropertyProviders(excludedHeartBeatPropertiesProvider);
  }

  /**
   * Gets the current state of heartbeat
   * @return true if enabled
   */
  public boolean isHeartBeatEnabled() {
    return this.heartBeatProviderInterface.isHeartBeatEnabled();
  }

  /**
   * Sets the state of heartbeat module
   * @param heartBeatEnabled boolean true / false
   */
  public void setHeartBeatEnabled(boolean heartBeatEnabled) {
    this.heartBeatProviderInterface.setHeartBeatEnabled(heartBeatEnabled);
  }

  @Override
  public void initialize(TelemetryConfiguration configuration) {
    if (!isInitialized && isHeartBeatEnabled()) {
      synchronized (lock) {
        if (!isInitialized && isHeartBeatEnabled()) {
          this.heartBeatProviderInterface.initialize(configuration);
          InternalLogger.INSTANCE.info("heartbeat is enabled");
          isInitialized = true;
        }
      }
    }
  }

  /**
   * Parses the input parameter value separated by ; into List.
   * @param value ; seperated value string
   * @return List representing individual values
   */
  private List<String> parseStringToList(String value) {
    if (value == null || value.length() == 0) return new ArrayList<>();
    return Arrays.asList(value.split(";"));
  }

}
