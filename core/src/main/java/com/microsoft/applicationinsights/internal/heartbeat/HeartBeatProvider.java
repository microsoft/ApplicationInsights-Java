package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
  private AtomicLong heartbeatsSent;

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
  private ExecutorService propertyUpdateService;

  /**
   * Threadpool used to send data heartbeat telemetry
   */
  private ScheduledExecutorService heartBeatSenderService;

  /**
   * Heartbeat enabled state
   */
  private volatile boolean isEnabled;

  private final Object lock = new Object();

  public HeartBeatProvider() {
    this.interval = HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL;
    this.heartbeatProperties = new ConcurrentHashMap<>();
    this.isEnabled = true;
    this.heartbeatsSent = new AtomicLong(0);
    this.propertyUpdateService = Executors.newCachedThreadPool(ThreadPoolUtils.createDaemonThreadFactory(HeartBeatProvider.class, "propertyUpdateService"));
    this.heartBeatSenderService = Executors.newScheduledThreadPool(1, ThreadPoolUtils.createDaemonThreadFactory(HeartBeatProvider.class, "heartBeatSenderService"));
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
    if (isEnabled) {
      if (this.telemetryClient == null) {
        this.telemetryClient = new TelemetryClient(configuration);
      }

      //Submit task to set properties to dictionary using separate thread. we do not wait for the
      //results to come out as some I/O bound properties may take time.
      propertyUpdateService.submit(HeartbeatDefaultPayload.populateDefaultPayload(getExcludedHeartBeatProperties(),
          getExcludedHeartBeatPropertyProviders(), this));

      heartBeatSenderService.scheduleAtFixedRate(heartBeatPulse(), interval, interval, TimeUnit.SECONDS);
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
             InternalLogger.INSTANCE.trace("added heartbeat property %s - %s", propertyName, propertyValue);
        }
        else {
          throw new Exception("heartbeat property cannot be added twice. Please use setHeartBeatProperty instead to modify the value");
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

        if (!heartbeatProperties.containsKey(propertyName)) {
          setResult = false;
          throw new Exception("heartbeat property cannot be set without adding it first");
        } else if (HeartbeatDefaultPayload.isDefaultKeyword(propertyName)) {
          setResult = false;
          throw new Exception("heartbeat beat property specified is a reserved property");
        }
        else  {
          HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
          payload.setHealthy(isHealthy);
          payload.setPayloadValue(propertyValue);
          heartbeatProperties.put(propertyName, payload);
          setResult = true;
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
    propertyUpdateService.shutdown();
    heartBeatSenderService.shutdown();
    try {
      propertyUpdateService.awaitTermination(timeout, timeUnit);
      heartBeatSenderService.awaitTermination(timeout, timeUnit);
    }
    catch (InterruptedException e) {
      InternalLogger.INSTANCE.warn("unable to successfully terminate heartbeat module, "
          + "encountered and exception with stacktrace, %s", ExceptionUtils.getStackTrace(e));
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
      heartbeatsSent.incrementAndGet();
      InternalLogger.INSTANCE.trace("No of heartbeats sent, %s", heartbeatsSent.get());
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
