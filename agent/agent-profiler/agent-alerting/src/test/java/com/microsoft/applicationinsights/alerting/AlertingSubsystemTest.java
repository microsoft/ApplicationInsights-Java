// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AlertingSubsystemTest {

  private static final AlertingConfig.RequestTrigger requestTrigger;
  static {
       requestTrigger = new AlertingConfig.RequestTrigger(
            "test",
            AlertingConfig.RequestTriggerType.LATENCY,
            new AlertingConfig.RequestFilter(AlertingConfig.RequestFilterType.NAME_REGEX, "/api/users/.*"),
            new AlertingConfig.RequestAggregation(AlertingConfig.RequestAggregationType.BREACH_RATIO, 7000,
                    new AlertingConfig.RequestAggregationConfig(10000, 10)),
            new AlertingConfig.RequestTriggerThreshold(AlertingConfig.RequestTriggerThresholdType.GREATER_THAN, 0.75f),
            new AlertingConfig.RequestTriggerThrottling(AlertingConfig.RequestTriggerThrottlingType.FIXED_DURATION_COOLDOWN, 1800),
            10);
  }

  private static AlertingSubsystem getAlertMonitor(
      Consumer<AlertBreach> consumer, TestTimeSource timeSource) {
    AlertingSubsystem monitor = AlertingSubsystem.create(consumer, timeSource);

    monitor.updateConfiguration(
        AlertingConfiguration.create(
            AlertConfiguration.builder()
                .setType(AlertMetricType.CPU)
                .setEnabled(true)
                .setThreshold((float) 80)
                .setProfileDurationSeconds(30)
                .setCooldownSeconds(14400)
                .build(),
            AlertConfiguration.builder()
                .setType(AlertMetricType.MEMORY)
                .setEnabled(true)
                .setThreshold((float) 20)
                .setProfileDurationSeconds(120)
                .setCooldownSeconds(14400)
                .build(),
            DefaultConfiguration.builder()
                .setSamplingEnabled(true)
                .setSamplingRate(5)
                .setSamplingProfileDuration(120)
                .build(),
            CollectionPlanConfiguration.builder()
                .setSingle(true)
                .setMode(EngineMode.immediate)
                .setExpiration(Instant.now())
                .setImmediateProfilingDurationSeconds(120)
                .setSettingsMoniker("a-settings-moniker")
                .build(),
            new ArrayList<AlertConfiguration>() {{
                  add(AlertConfiguration.builder()
                          .setType(AlertMetricType.REQUEST)
                          .setEnabled(true)
                          .setThreshold(0.75f)
                          .setProfileDurationSeconds(10)
                          .setCooldownSeconds(1800)
                          .setRequestTrigger(requestTrigger)
                          .build());
                }}));
    return monitor;
  }

  @Test
  void alertTriggerIsCalled() {

    AtomicReference<AlertBreach> called = new AtomicReference<>();
    Consumer<AlertBreach> consumer = called::set;
    TestTimeSource timeSource = new TestTimeSource();

    AlertingSubsystem service = getAlertMonitor(consumer, timeSource);

    for (int i = 0; i < 10; i++) {
      service.track(AlertMetricType.CPU, 90.0);
    }
    timeSource.increment(50000);
    service.track(AlertMetricType.CPU, 90.0);

    assertThat(called.get().getType()).isEqualTo(AlertMetricType.CPU);
    assertThat(called.get().getAlertValue()).isEqualTo(90.0);
  }

  @Test
  void manualAlertWorks() {
    AtomicReference<AlertBreach> called = new AtomicReference<>();
    Consumer<AlertBreach> consumer = called::set;
    TestTimeSource timeSource = new TestTimeSource();

    AlertingSubsystem service = AlertingSubsystem.create(consumer, timeSource);

    service.updateConfiguration(
        AlertingConfiguration.create(
            AlertConfiguration.builder()
                .setType(AlertMetricType.CPU)
                .setEnabled(true)
                .setThreshold((float) 80)
                .setProfileDurationSeconds(30)
                .setCooldownSeconds(14400)
                .build(),
            AlertConfiguration.builder()
                .setType(AlertMetricType.MEMORY)
                .setEnabled(true)
                .setThreshold((float) 20)
                .setProfileDurationSeconds(120)
                .setCooldownSeconds(14400)
                .build(),
            DefaultConfiguration.builder()
                .setSamplingEnabled(true)
                .setSamplingRate(5)
                .setSamplingProfileDuration(120)
                .build(),
            CollectionPlanConfiguration.builder()
                .setSingle(true)
                .setMode(EngineMode.immediate)
                .setExpiration(Instant.now().plus(100, ChronoUnit.SECONDS))
                .setImmediateProfilingDurationSeconds(120)
                .setSettingsMoniker("a-settings-moniker")
                .build(),
            new ArrayList<AlertConfiguration>() {{
                  add(AlertConfiguration.builder()
                          .setType(AlertMetricType.REQUEST)
                          .setEnabled(true)
                          .setThreshold(0.75f)
                          .setProfileDurationSeconds(10)
                          .setCooldownSeconds(1800)
                          .setRequestTrigger(requestTrigger)
                          .build());
                }}));

    assertThat(called.get().getType()).isEqualTo(AlertMetricType.MANUAL);
  }

  @Test
  void manualAlertDoesNotTriggerAfterExpired() {
    AtomicReference<AlertBreach> called = new AtomicReference<>();
    Consumer<AlertBreach> consumer = called::set;
    TestTimeSource timeSource = new TestTimeSource();

    AlertingSubsystem service = AlertingSubsystem.create(consumer, timeSource);

    service.updateConfiguration(
        AlertingConfiguration.create(
            AlertConfiguration.builder()
                .setType(AlertMetricType.CPU)
                .setEnabled(true)
                .setThreshold((float) 80)
                .setProfileDurationSeconds(30)
                .setCooldownSeconds(14400)
                .build(),
            AlertConfiguration.builder()
                .setType(AlertMetricType.MEMORY)
                .setEnabled(true)
                .setThreshold((float) 20)
                .setProfileDurationSeconds(120)
                .setCooldownSeconds(14400)
                .build(),
            DefaultConfiguration.builder()
                .setSamplingEnabled(true)
                .setSamplingRate(5)
                .setSamplingProfileDuration(120)
                .build(),
            CollectionPlanConfiguration.builder()
                .setSingle(true)
                .setMode(EngineMode.immediate)
                .setExpiration(Instant.now().minus(100, ChronoUnit.SECONDS))
                .setImmediateProfilingDurationSeconds(120)
                .setSettingsMoniker("a-settings-moniker")
                .build(),
            new ArrayList<AlertConfiguration>() {{
                  add(AlertConfiguration.builder()
                          .setType(AlertMetricType.REQUEST)
                          .setEnabled(true)
                          .setThreshold(0.75f)
                          .setProfileDurationSeconds(10)
                          .setCooldownSeconds(1800)
                          .setRequestTrigger(requestTrigger)
                          .build());
                }}));

    assertThat(called.get()).isNull();
  }
}
