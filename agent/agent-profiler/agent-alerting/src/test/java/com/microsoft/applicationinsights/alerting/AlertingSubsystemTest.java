// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AlertingSubsystemTest {

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
                .build()));
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
                .build()));

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
                .build()));

    assertThat(called.get()).isNull();
  }
}
