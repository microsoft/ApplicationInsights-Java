package com.microsoft.applicationinsights.alerting.analysis;

import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains a pipeline that receives telemetry, feeds it into the analysis pipeline (starting at the rolling average)
 */
public class AlertPipeline implements AlertPipelineMXBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertPipeline.class);
    private static final String JMX_KEY = "com.microsoft:type=AI-alert,name=";

    private final RollingAverage rollingAverage;
    private final Consumer<AlertBreach> alertObserver;

    private AlertConfiguration alertConfiguration;
    private AlertPipelineTrigger alertTrigger;

    public AlertPipeline(RollingAverage rollingAverage, Consumer<AlertBreach> alertObserver,
                         AlertConfiguration alertConfiguration) {
        this.rollingAverage = rollingAverage;
        this.alertObserver = alertObserver;
        this.alertConfiguration = alertConfiguration;
    }

    public static AlertPipeline create(RollingAverage rollingAverage, AlertConfiguration alertConfiguration, Consumer<AlertBreach> alertObserver) {
        AlertPipeline trigger = new AlertPipeline(rollingAverage, alertObserver, alertConfiguration);
        trigger.registerMBean();

        trigger.setAlertTrigger(rollingAverage, alertConfiguration, alertObserver);
        return trigger;
    }

    private void registerMBean() {
        try {
            ObjectName objectName = new ObjectName(JMX_KEY + alertConfiguration.getType().name());

            try {
                MBeanInfo existing = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(objectName);
                if (existing != null) {
                    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
                }
            } catch (Exception e) {
                //Expected if mbean does not exist
            }

            ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);

        } catch (Exception e) {
            LOGGER.error("Failed to register MBEAN", e);
        }
    }

    private void setAlertTrigger(RollingAverage rollingAverage, AlertConfiguration newAlertConfig, Consumer<AlertBreach> alertObserver) {
        this.alertTrigger = new AlertPipelineTrigger(newAlertConfig, alertObserver);
        rollingAverage.setConsumer(alertTrigger);
    }

    public OptionalDouble calculateAverage() {
        return rollingAverage.calculateAverage();
    }

    public void updateConfig(AlertConfiguration newAlertConfig) {
        this.alertConfiguration = newAlertConfig;
        setAlertTrigger(rollingAverage, newAlertConfig, alertObserver);
    }

    public void track(TelemetryDataPoint telemetryDataPoint) {
        rollingAverage.track(telemetryDataPoint);
    }

    @Override public long getCoolDown() {
        return alertConfiguration.getCooldown();
    }

    @Override public long getRollingAverageWindow() {
        return rollingAverage.getWindowLengthInSec();
    }

    @Override public long getProfilerDuration() {
        return alertConfiguration.getProfileDuration();
    }

    @Override public float getThreshold() {
        return alertConfiguration.getThreshold();
    }

    @Override public double getCurrentAverage() {
        return calculateAverage().orElse(0.0d);
    }

    @Override public boolean getEnabled() {
        return alertConfiguration.isEnabled();
    }

    @Override public boolean isOffCooldown() {
        return this.alertTrigger.isOffCooldown();
    }

    @Override public String getLastAlertTime() {
        return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(this.alertTrigger.getLastAlertTime());
    }
}
