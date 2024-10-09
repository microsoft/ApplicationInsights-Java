// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertConfigParserTest {

  @Test
  void nullsInConfigAreHandled() {
    AlertingConfiguration config = AlertConfigParser.parse(null, null, null, null, null);
    assertThat(config.getCpuAlert().isEnabled()).isFalse();
    assertThat(config.getCollectionPlanConfiguration().isSingle()).isFalse();
    assertThat(config.getMemoryAlert().isEnabled()).isFalse();
    assertThat(config.getDefaultConfiguration().getSamplingEnabled()).isFalse();
    assertThat(config.getRequestAlertConfiguration()).isEmpty();
  }

  @Test
  void saneDataIsParsed() {
    AlertingConfiguration config =
        AlertConfigParser.parse(
            "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400",
            "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400",
            "--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120",
            "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker",
            null);

    assertThat(config.getCpuAlert())
        .isEqualTo(
            AlertConfiguration.builder()
                .setType(AlertMetricType.CPU)
                .setEnabled(true)
                .setThreshold((float) 80)
                .setProfileDurationSeconds(30)
                .setCooldownSeconds(14400)
                .build());

    assertThat(config.getMemoryAlert())
        .isEqualTo(
            AlertConfiguration.builder()
                .setType(AlertMetricType.MEMORY)
                .setEnabled(true)
                .setThreshold((float) 20)
                .setProfileDurationSeconds(120)
                .setCooldownSeconds(14400)
                .build());
    assertThat(config.getDefaultConfiguration())
        .isEqualTo(
            DefaultConfiguration.builder()
                .setSamplingEnabled(true)
                .setSamplingRate(5)
                .setSamplingProfileDuration(120)
                .build());
    assertThat(config.getCollectionPlanConfiguration())
        .isEqualTo(
            CollectionPlanConfiguration.builder()
                .setSingle(true)
                .setMode(EngineMode.immediate)
                .setExpiration(AlertConfigParser.parseBinaryDate(5249157885138288517L))
                .setImmediateProfilingDurationSeconds(120)
                .setSettingsMoniker("a-settings-moniker")
                .build());
  }

  @Test
  void requestTriggerIsBuilt() {
    AlertingConfig.RequestTrigger requestTrigger =
        new AlertingConfig.RequestTrigger()
            .setName("test")
            .setType(AlertingConfig.RequestTriggerType.LATENCY)
            .setFilter(
                new AlertingConfig.RequestFilter()
                    .setType(AlertingConfig.RequestFilterType.NAME_REGEX)
                    .setValue("/api/users/.*"))
            .setAggregation(
                new AlertingConfig.RequestAggregation()
                    .setType(AlertingConfig.RequestAggregationType.BREACH_RATIO)
                    .setWindowSizeMillis(7000)
                    .setConfiguration(
                        new AlertingConfig.RequestAggregationConfig()
                            .setThresholdMillis(10000)
                            .setMinimumSamples(10)))
            .setThreshold(
                new AlertingConfig.RequestTriggerThreshold()
                    .setType(AlertingConfig.RequestTriggerThresholdType.GREATER_THAN)
                    .setValue(0.75f))
            .setThrottling(
                new AlertingConfig.RequestTriggerThrottling()
                    .setType(AlertingConfig.RequestTriggerThrottlingType.FIXED_DURATION_COOLDOWN)
                    .setValue(1800))
            .setProfileDuration(10);

    List<AlertingConfig.RequestTrigger> requestTriggers = new ArrayList<>();
    requestTriggers.add(requestTrigger);

    AlertingConfiguration config =
        AlertConfigParser.parse(
            "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400",
            "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400",
            "--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120",
            "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker",
            requestTriggers);

    assertThat(config.getRequestAlertConfiguration()).isNotNull();
    assertThat(config.getRequestAlertConfiguration().size()).isEqualTo(1);
    assertThat(config.getRequestAlertConfiguration().get(0))
        .isEqualTo(
            AlertConfiguration.builder()
                .setType(AlertMetricType.REQUEST)
                .setEnabled(true)
                .setThreshold(0.75f)
                .setProfileDurationSeconds(10)
                .setCooldownSeconds(1800)
                .setRequestTrigger(requestTrigger)
                .build());
  }
}
