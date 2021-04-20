package com.microsoft.applicationinsights.internal.heartbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.TelemetryUtil.createMetricsData;

/**
 * <p>
 *  Concrete implementation of Heartbeat functionality. This class implements
 *  {@link com.microsoft.applicationinsights.internal.heartbeat.HeartBeatProviderInterface}
 * </p>
 *
 * @author Dhaval Doshi
 */
public class HeartBeatProvider implements HeartBeatProviderInterface {

  private static final Logger logger = LoggerFactory.getLogger(HeartBeatProvider.class);

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
  private final ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

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
  private final ExecutorService propertyUpdateService;

  /**
   * Threadpool used to send data heartbeat telemetry
   */
  private final ScheduledExecutorService heartBeatSenderService;

  /**
   * Heartbeat enabled state
   */
  private volatile boolean isEnabled;

  public HeartBeatProvider() {
    this.interval = HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL;
    this.heartbeatProperties = new ConcurrentHashMap<>();
    this.isEnabled = true;
    this.heartbeatsSent = 0;
    this.propertyUpdateService = Executors.newCachedThreadPool(ThreadPoolUtils.createDaemonThreadFactory(HeartBeatProvider.class, "propertyUpdateService"));
    this.heartBeatSenderService = Executors.newSingleThreadScheduledExecutor( ThreadPoolUtils.createDaemonThreadFactory(HeartBeatProvider.class, "heartBeatSenderService"));
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
      if (!heartbeatProperties.containsKey(propertyName)) {
           HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
           payload.setHealthy(isHealthy);
           payload.setPayloadValue(propertyValue);
           heartbeatProperties.put(propertyName, payload);
           isAdded = true;
           logger.trace("added heartbeat property {} - {}", propertyName, propertyValue);
      }
      else {
        logger.trace("heartbeat property {} cannot be added twice. Please use setHeartBeatProperty instead to modify the value",
            propertyName);
      }
    }
    else {
      logger.warn("cannot add property without property name");
    }
    return isAdded;
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

  /**
   * Send the heartbeat item synchronously to application insights backend.
   */
  private void send() {

    MetricsData telemetry = gatherData();
    telemetry.getProperties().put(ContextTagKeys.AI_OPERATION_SYNTHETIC_SOURCE.toString(), HEARTBEAT_SYNTHETIC_METRIC_NAME);
    telemetryClient.track(telemetry);
    logger.trace("No of heartbeats sent, {}", ++heartbeatsSent);

  }

  /**
   * Creates and returns the heartbeat telemetry.
   * @return Metric Telemetry which represent heartbeat.
   */
  private MetricsData gatherData() {

    MetricsData heartbeat = createMetricsData(HEARTBEAT_SYNTHETIC_METRIC_NAME, 0.0);
    Map<String, String> property = heartbeat.getProperties();
    for (Map.Entry<String, HeartBeatPropertyPayload> entry : heartbeatProperties.entrySet()) {
      property.put(entry.getKey(), entry.getValue().getPayloadValue());
      double currentValue = heartbeat.getMetrics().get(0).getValue();
      currentValue += entry.getValue().isHealthy() ? 0 : 1;
      heartbeat.getMetrics().get(0).setValue(currentValue);
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
        try {
         send();
        }
        catch (Exception e) {
          logger.warn("Error occured while sending heartbeat");
        }
      }
    };
  }

}
