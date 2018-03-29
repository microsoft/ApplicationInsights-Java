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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class HeartBeatProvider implements HeartBeatProviderInterface, Stoppable {

  private final String HEARTBEAT_SYNTHETIC_METRIC_NAME = "HeartbeatState";

  private List<String> disableDefaultProperties = new ArrayList<>();

  private List<String> disabledHeartBeatPropertiesProviders = new ArrayList<>();

  private long heartbeatsSent;

  private ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

  private long interval;

  private TelemetryClient telemetryClient;

  private ExecutorService executorService = Executors.newCachedThreadPool();

  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

  private volatile boolean isEnabled;

  private Lock lock = new ReentrantLock();

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

//    try {
      executorService.submit(HeartbeatDefaultPayload.populateDefaultPayload(getExcludedHeartBeatProperties(),
          getExcludedHeartBeatPropertyProviders(), this));
//    }
//    catch (InterruptedException e) {
//
//    }
//    catch (ExecutionException e) {
//
//    }

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
        if (heartbeatProperties.containsKey(propertyName)) {
          HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
          payload.setHealthy(isHealthy);
          payload.setPayloadValue(propertyValue);
          heartbeatProperties.put(propertyName, payload);
          setResult = true;
        }
        else {
          throw new Exception("Cannot set heartbeat property without adding it first");
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

  private void send() {

    lock.lock();
    try {
      MetricTelemetry telemetry = (MetricTelemetry)gatherData();
      telemetry.getContext().getOperation().setSyntheticSource(HEARTBEAT_SYNTHETIC_METRIC_NAME);
      telemetryClient.trackMetric(telemetry);
      InternalLogger.INSTANCE.trace("sent heart beat");
    }
    finally{
      lock.unlock();
    }
  }

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
