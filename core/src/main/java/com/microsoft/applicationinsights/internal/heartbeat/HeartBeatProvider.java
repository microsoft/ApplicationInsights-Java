package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <p>
 *  Concrete implementation of Heartbeat functionality. This class implements
 *  {@link com.microsoft.applicationinsights.internal.heartbeat.HeartBeatProviderInterface} and
 *  {@link com.microsoft.applicationinsights.internal.shutdown.Stoppable}
 * </p>
 *
 * @author Dhaval Doshi
 * @since 03-30-2018
 */
public class HeartBeatProvider implements HeartBeatProviderInterface, Stoppable {

  /**
   * The name of the heartbeat metric.
   */
  private final String HEARTBEAT_SYNTHETIC_METRIC_NAME = "HeartbeatState";

  /**
   * The list of disabled properties
   */
  private List<String> disableDefaultProperties = new ArrayList<>();

  /**
   * List of disabled heartbeat providers
   */
  private List<String> disabledHeartBeatPropertiesProviders = new ArrayList<>();

  /**
   * The counter for heartbeat sent to portal
   */
  private long heartbeatsSent;

  /**
   * Map to hold heartbeat properties
   */
  private ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

  /**
   * Interval at which heartbeat would be sent
   */
  private long interval;

  /**
   * Telemetry client instance used to send heartbeat.
   */
  private TelemetryClient telemetryClient;

  /**
   * ThreadPool used for adding properties to concurrent dictionary
   */
  private ExecutorService executorService = Executors.newCachedThreadPool();

  /**
   * Threadpool used to send data heartbeat telemetry
   */
  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

  /**
   * Heartbeat enabled state
   */
  private volatile boolean isEnabled;

  private final Object lock = new Object();

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

    //Submit task to set properties to dictionary using separate thread. we do not wait for the
    //results to come out as some I/O bound properties may take time.
    executorService.submit(HeartbeatDefaultPayload.populateDefaultPayload(getExcludedHeartBeatProperties(),
        getExcludedHeartBeatPropertyProviders(), this));


    if (isEnabled) {
      scheduledExecutorService.scheduleAtFixedRate(heartBeatPulse(), interval, interval, TimeUnit.SECONDS);
    }

  }

  @Override
  public boolean addHeartBeatProperty(String propertyName, String propertyValue,
      boolean isHealthy) {

    boolean isAdded= false;
    if (!StringUtils.isEmpty(propertyName)) {
      try {
        if (!heartbeatProperties.containsKey(propertyName)) {
             HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
             payload.setHealthy(isHealthy);
             payload.setPayloadValue(propertyValue);
             heartbeatProperties.put(propertyName, payload);
             isAdded = true;
             InternalLogger.INSTANCE.trace("added heartbeat property");
        }
        else {
          throw new Exception("heartbeat property cannot be added twice");
        }
      } catch (Exception e) {
        InternalLogger.INSTANCE.warn("Failed to add the property %s value %s, stack trace is : %s," ,
            propertyName, propertyValue, ExceptionUtils.getStackTrace(e));
      }
    }
    else {
      InternalLogger.INSTANCE.warn("cannot add property without property name");
    }
    return isAdded;
  }

  @Override
  public boolean setHeartBeatProperty(String propertyName, String propertyValue,
      boolean isHealthy) {

    boolean setResult = false;
    if (!StringUtils.isEmpty(propertyName)) {
      try {
        if (heartbeatProperties.containsKey(propertyName) && !HeartbeatDefaultPayload.isDefaultKeyWord(propertyName)) {
          HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
          payload.setHealthy(isHealthy);
          payload.setPayloadValue(propertyValue);
          heartbeatProperties.put(propertyName, payload);
          setResult = true;
        }
        else {
          throw new Exception("heartbeat property cannot be set without adding it first, or heartbeat"
              + "beat property specified is a reserved property");
        }
      }
      catch (Exception e) {
        InternalLogger.INSTANCE.warn("failed to set heartbeat property name %s, value %s, "
            + "stack trace is: %s", propertyName, propertyValue, ExceptionUtils.getStackTrace(e));
      }
    }
    else {
      InternalLogger.INSTANCE.warn("cannot set property without property name");
    }
    return setResult;
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

  /**
   * Send the heartbeat item synchronously to application insights backend.
   */
  private void send() {

    synchronized (lock) {
      MetricTelemetry telemetry = (MetricTelemetry)gatherData();
      telemetry.getContext().getOperation().setSyntheticSource(HEARTBEAT_SYNTHETIC_METRIC_NAME);
      telemetryClient.trackMetric(telemetry);
      InternalLogger.INSTANCE.trace("sent heart beat");
    }

  }

  /**
   * Creates and returns the heartbeat telemetry.
   * @return Metric Telemetry which represent heartbeat.
   */
  private Telemetry gatherData() {

    MetricTelemetry heartbeat = new MetricTelemetry(HEARTBEAT_SYNTHETIC_METRIC_NAME, 0.0);
    Map<String, String> property = heartbeat.getProperties();
    for (Map.Entry<String, HeartBeatPropertyPayload> entry : heartbeatProperties.entrySet()) {
      property.put(entry.getKey(), entry.getValue().getPayloadValue());
      double currentValue = heartbeat.getValue();
      currentValue += entry.getValue().isHealthy() ? 0 : 1;
      heartbeat.setValue(currentValue);
      ++heartbeatsSent;
    }
    return heartbeat;
  }

  /**
   * Runnable which is responsible for calling the send method to transmit telemetry
   * @return Runnable which has logic to send heartbeat.
   */
  private Runnable heartBeatPulse() {
    return new Runnable() {
      @Override
      public void run() {
        if (isEnabled) {

          try {
           send();
          }
          catch (Exception e) {
            InternalLogger.INSTANCE.warn("Error occured while sending heartbeat");
          }
        }
        else {
          InternalLogger.INSTANCE.info("Heartbeat is disabled");
        }
      }
    };
  }
}
