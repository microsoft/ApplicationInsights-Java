package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <h1>Interface for HeartBeat Properties</h1>
 *
 * <p>
 * This interface defines an implementation for configuring the properties of Application Insights
 * SDK. It allows users to set and get the configuration properties of HeartBeat module. A user can
 * create or bring his own custom implementation of Heartbeat module if wished provided that he abides
 * to the contracts set by this Interface.
 *
 * Default concrete Implementation {@link com.microsoft.applicationinsights.internal.heartbeat.HeartBeatProvider}
 * </p>
 *
 * @author Dhaval Doshi
 * @since 03-30-2018
 */
public interface HeartBeatProviderInterface {

  /**
   * Default interval in seconds to transmit heartbeat pulse.
   */
  long DEFAULT_HEARTBEAT_INTERVAL = TimeUnit.MINUTES.toSeconds(15);

  /**
   * Minimum interval which can be configured by user to transmit heartbeat pulse.
   */
  long MINIMUM_HEARTBEAT_INTERVAL = TimeUnit.SECONDS.toSeconds(30);

  /**
   * Gets the instrumentation key used by telemetry client sending heartbeat.
   * @return InstrumentationKey
   */
  String getInstrumentationKey();

  /**
   * Sets the instrumentation key
   * @param key Key to be set
   */
  void setInstrumentationKey(String key);

  /**
   * This method initializes the concrete module.
   * @param configuration TelemetryConfiguration
   */
  void initialize(TelemetryConfiguration configuration);

  /**
   * Adds the heartbeat property to the heartbeat payload.
   * @param propertyName Name of the property to be added in Heartbeat payload
   * @param propertyValue Value of the property to be added in Heartbeat payload
   * @param isHealthy indicates if heartbeat is healthy
   * @return true if property is added successfully
   */
  boolean addHeartBeatProperty(String propertyName, String propertyValue, boolean isHealthy);

  /**
   * Sets the value of already existing heartbeat property in the payload.
   * @param propertyName Name of the property to be added in Heartbeat payload
   * @param propertyValue Value of the property to be added in Heartbeat payload
   * @param isHealthy indicates if heartbeat is healthy
   * @return true if property is added successfully
   */
  boolean setHeartBeatProperty(String propertyName, String propertyValue, boolean isHealthy);

  /**
   * Returns if heartbeat is enabled or not.
   * @return true if heartbeat is enabled
   */
  boolean isHeartBeatEnabled();

  /**
   * Enables or disables heartbeat module.
   * @param isEnabled state of the heartbeat (enabled/disabled)
   */
  void setHeartBeatEnabled(boolean isEnabled);

  /**
   * This returns the list of Excluded Heart Beat Providers
   * @return list of excluded heartbeat providers
   */
  List<String> getExcludedHeartBeatPropertyProviders();

  /**
   * Sets the list of excluded heartbeat providers.
   * @param excludedHeartBeatPropertyProviders List of heartbeat providers to be excluded
   */
  void setExcludedHeartBeatPropertyProviders(List<String> excludedHeartBeatPropertyProviders);

  /**
   * Gets the currently set heartbeat interval.
   * @return returns the time interval of heartbeat
   */
  long getHeartBeatInterval();

  /**
   * Sets the time interval of heartbeat in seconds.
   * @param timeUnit Heartbeat interval in seconds
   */
  void setHeartBeatInterval(long timeUnit);

  /**
   * Returns the list of excluded heartbeat properties.
   * @return List of excluded heartbeat properties
   */
  List<String> getExcludedHeartBeatProperties();

  /**
   * Sets the list of properties to be excluded from heartbeat payload.
   * @param excludedHeartBeatProperties  List of properties to be excluded
   */
  void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties);

}
