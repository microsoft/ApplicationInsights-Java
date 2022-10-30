// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.AlertPipelineTrigger;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AlertTriggerTest {

  @Test
  void underThresholdDataDoesNotTrigger() {

    AlertConfiguration config =
        AlertConfiguration.builder()
            .setType(AlertMetricType.CPU)
            .setEnabled(true)
            .setThreshold(0.5f)
            .setProfileDurationSeconds(1)
            .setCooldownSeconds(1000)
            .build();
    AtomicBoolean called = new AtomicBoolean(false);
    AlertPipelineTrigger trigger = getAlertTrigger(config, called);
    for (int i = 0; i < 100; i++) {
      trigger.accept(0.4);
    }

    assertThat(called.get()).isFalse();
  }

  @Test
  void overThresholdDataDoesTrigger() {

    AlertConfiguration config =
        AlertConfiguration.builder()
            .setType(AlertMetricType.CPU)
            .setEnabled(true)
            .setThreshold(0.5f)
            .setProfileDurationSeconds(1)
            .setCooldownSeconds(1)
            .build();
    AtomicBoolean called = new AtomicBoolean(false);
    AlertPipelineTrigger trigger = getAlertTrigger(config, called);

    for (int i = 0; i < 100; i++) {
      trigger.accept(0.51);
    }

    assertThat(called.get()).isTrue();
  }

  @Test
  void doesNotReTriggerDueToCooldown() {

    AlertConfiguration config =
        AlertConfiguration.builder()
            .setType(AlertMetricType.CPU)
            .setEnabled(true)
            .setThreshold(0.5f)
            .setProfileDurationSeconds(1)
            .setCooldownSeconds(1000)
            .build();
    AtomicBoolean called = new AtomicBoolean(false);
    AlertPipelineTrigger trigger = getAlertTrigger(config, called);

    for (int i = 0; i < 100; i++) {
      trigger.accept(0.51);
    }
    assertThat(called.get()).isTrue();
    called.set(false);

    for (int i = 0; i < 100; i++) {
      trigger.accept(0.1);
    }

    for (int i = 0; i < 100; i++) {
      trigger.accept(0.51);
    }

    assertThat(called.get()).isFalse();
  }

  @Test
  void doesNotReTriggerAfterCooldown() throws InterruptedException {

    AlertConfiguration config =
        AlertConfiguration.builder()
            .setType(AlertMetricType.CPU)
            .setEnabled(true)
            .setThreshold(0.5f)
            .setProfileDurationSeconds(1)
            .setCooldownSeconds(1)
            .build();
    AtomicBoolean called = new AtomicBoolean(false);
    AlertPipelineTrigger trigger = getAlertTrigger(config, called);

    for (int i = 0; i < 100; i++) {
      trigger.accept(0.51);
    }
    assertThat(called.get()).isTrue();
    called.set(false);

    Thread.sleep(2000);

    for (int i = 0; i < 100; i++) {
      trigger.accept(0.1);
    }

    for (int i = 0; i < 100; i++) {
      trigger.accept(0.51);
    }

    assertThat(called.get()).isTrue();
  }

  private static AlertPipelineTrigger getAlertTrigger(
      AlertConfiguration config, AtomicBoolean called) {
    Consumer<AlertBreach> consumer =
        alert -> {
          assertThat(alert.getType()).isEqualTo(AlertMetricType.CPU);
          assertThat(alert.getAlertConfiguration()).isEqualTo(config);
          called.set(true);
        };

    return new AlertPipelineTrigger(config, consumer);
  }
}
