// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.function.Consumer;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX MBean that exposes profile triggering via JMX tools.
 *
 * <p>Usage via jmxterm (any JDK):
 *
 * <pre>
 *   echo "run -b com.microsoft:type=AI-alert,name=ProfilerControl triggerProfile" | \
 *     java -jar jmxterm.jar -l &lt;pid&gt;
 * </pre>
 *
 * <p>Or connect with JConsole and invoke {@code triggerProfile()} on the {@code
 * com.microsoft:type=AI-alert,name=ProfilerControl} MBean.
 */
public class ProfilerControl implements ProfilerControlMBean {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerControl.class);

  private static final String OBJECT_NAME = "com.microsoft:type=AI-alert,name=ProfilerControl";
  private static final int DEFAULT_DURATION_SECONDS = 120;

  private final Consumer<AlertBreach> alertHandler;

  /**
   * Creates a new ProfilerControl instance.
   *
   * @param alertHandler consumer that processes the generated {@link AlertBreach}, typically wired
   *     to the profiler's recording logic
   */
  ProfilerControl(Consumer<AlertBreach> alertHandler) {
    this.alertHandler = alertHandler;
  }

  @Override
  public String triggerProfile() {
    return triggerProfile(DEFAULT_DURATION_SECONDS);
  }

  @Override
  public String triggerProfile(int durationSeconds) {
    if (durationSeconds <= 0) {
      return "Error: duration must be positive, got " + durationSeconds;
    }

    logger.info("Manual profile triggered via JMX, duration={}s", durationSeconds);

    AlertBreach alertBreach =
        AlertBreach.builder()
            .setType(AlertMetricType.MANUAL)
            .setAlertValue(0.0)
            .setAlertConfiguration(
                AlertConfiguration.builder()
                    .setType(AlertMetricType.MANUAL)
                    .setEnabled(true)
                    .setProfileDurationSeconds(durationSeconds)
                    .build())
            .setProfileId(UUID.randomUUID().toString())
            .setCpuMetric(0)
            .setMemoryUsage(0)
            .build();

    alertHandler.accept(alertBreach);
    return "Profile recording started (duration="
        + durationSeconds
        + "s, id="
        + alertBreach.getProfileId()
        + ")";
  }

  /**
   * Registers this MBean with the platform MBean server. Call during profiler initialization.
   *
   * @param alertHandler the alert handler (typically wired to Profiler.accept)
   */
  public static void register(Consumer<AlertBreach> alertHandler) {
    try {
      MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName objectName = new ObjectName(OBJECT_NAME);
      ProfilerControl bean = new ProfilerControl(alertHandler);
      beanServer.registerMBean(bean, objectName);
      logger.info(
          "Registered profiler control MBean: {}. "
              + "Trigger profiles via JMX tools (e.g. jmxterm or JConsole).",
          OBJECT_NAME);
    } catch (InstanceAlreadyExistsException e) {
      logger.debug("Profiler control MBean already registered");
    } catch (Exception e) {
      logger.warn("Failed to register profiler control MBean", e);
    }
  }
}
