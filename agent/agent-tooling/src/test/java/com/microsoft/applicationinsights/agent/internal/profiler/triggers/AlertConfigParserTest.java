// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.profiler.config.TargetedCollectionPlan;
import com.microsoft.applicationinsights.agent.internal.profiler.config.TargetedInstance;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import com.microsoft.applicationinsights.alerting.config.TargetedCollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.TargetedInstanceConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                .setThreshold(80.0f)
                .setProfileDurationSeconds(30)
                .setCooldownSeconds(14400)
                .build());

    assertThat(config.getMemoryAlert())
        .isEqualTo(
            AlertConfiguration.builder()
                .setType(AlertMetricType.MEMORY)
                .setEnabled(true)
                .setThreshold(20.0f)
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

  @Test
  void targetedRolesAreParsedFaithfully() {
    ProfilerConfiguration profilerConfiguration =
        targetedConfiguration(targetedPlan().setRoles(Arrays.asList(" frontend ", "backend")));

    TargetedCollectionPlanConfiguration plan =
        AlertConfigParser.toAlertingConfig(profilerConfiguration)
            .getTargetedCollectionPlanConfiguration();

    assertThat(plan).isNotNull();
    assertThat(plan.getRoles()).containsExactly(" frontend ", "backend");
    assertThat(plan.getImmediateProfilingDurationSeconds()).isEqualTo(120);
    assertThat(plan.getExpiration()).isEqualTo(Instant.parse("2099-08-17T19:00:00Z"));
    assertThat(plan.getSettingsMoniker()).isEqualTo("Portal_test");
  }

  @Test
  void targetedInstancesAreParsedFaithfully() {
    ProfilerConfiguration profilerConfiguration =
        targetedConfiguration(
            targetedPlan()
                .setInstances(
                    Collections.singletonList(
                        new TargetedInstance().setRole("frontend").setName("instance-1"))));

    TargetedCollectionPlanConfiguration plan =
        AlertConfigParser.toAlertingConfig(profilerConfiguration)
            .getTargetedCollectionPlanConfiguration();

    assertThat(plan).isNotNull();
    assertThat(plan.getInstances())
        .containsExactly(TargetedInstanceConfiguration.create("frontend", "instance-1"));
  }

  @Test
  void malformedLegacyPlanDoesNotBlockTargetedPlan() {
    ProfilerConfiguration profilerConfiguration =
        targetedConfiguration(targetedPlan().setRoles(Collections.singletonList("frontend")))
            .setCollectionPlan(
                "--single --mode immediate --immediate-profiling-duration invalid"
                    + " --expiration invalid --settings-moniker legacy");

    AlertingConfiguration config = AlertConfigParser.toAlertingConfig(profilerConfiguration);

    assertThat(config.getCollectionPlanConfiguration().isSingle()).isFalse();
    assertThat(config.getTargetedCollectionPlanConfiguration()).isNotNull();
    assertThat(
            config.hasAnEnabledTrigger(
                "frontend", "instance-1", Instant.parse("2099-01-01T00:00:00Z")))
        .isTrue();
  }

  @Test
  void invalidTargetedPlansFailClosed() {
    TargetedCollectionPlan mixedPlan =
        targetedPlan()
            .setRoles(Collections.singletonList("frontend"))
            .setInstances(
                Collections.singletonList(
                    new TargetedInstance().setRole("frontend").setName("instance-1")));
    ProfilerConfiguration mixedConfiguration = targetedConfiguration(mixedPlan);

    AlertingConfiguration mixedAlertingConfig =
        AlertConfigParser.toAlertingConfig(mixedConfiguration);
    assertThat(mixedAlertingConfig.getTargetedCollectionPlanConfiguration()).isNotNull();
    assertThat(
            mixedAlertingConfig.hasAnEnabledTrigger(
                "frontend", "instance-1", Instant.parse("2099-01-01T00:00:00Z")))
        .isFalse();

    ProfilerConfiguration mixedLegacyConfiguration =
        targetedConfiguration(targetedPlan().setRoles(Collections.singletonList("frontend")))
            .setCollectionPlan(
                "--single --mode immediate --immediate-profiling-duration 120"
                    + " --expiration 5249157885138288517 --settings-moniker legacy");

    AlertingConfiguration combinedConfig =
        AlertConfigParser.toAlertingConfig(mixedLegacyConfiguration);
    assertThat(combinedConfig.getCollectionPlanConfiguration().isSingle()).isTrue();
    assertThat(combinedConfig.getTargetedCollectionPlanConfiguration()).isNotNull();
    assertThat(
            combinedConfig.hasAnEnabledTrigger(
                "frontend", "instance-1", Instant.parse("2099-01-01T00:00:00Z")))
        .isTrue();

    ProfilerConfiguration invalidTargetedWithLegacy =
        targetedConfiguration(mixedPlan)
            .setCollectionPlan(
                "--single --mode immediate --immediate-profiling-duration 120"
                    + " --expiration 5249157885138288517 --settings-moniker legacy");
    assertThat(
            AlertConfigParser.toAlertingConfig(invalidTargetedWithLegacy)
                .hasAnEnabledTrigger("frontend", "instance-1", Instant.EPOCH))
        .isFalse();
  }

  @Test
  void targetedPlansWithNullValuesFailClosed() {
    assertTargetedPlanInvalid(targetedPlan().setInstances(Collections.singletonList(null)));
    assertTargetedPlanInvalid(
        targetedPlan()
            .setInstances(
                Collections.singletonList(new TargetedInstance().setRole(null).setName(null))));
    assertTargetedPlanInvalid(
        targetedPlan()
            .setRoles(Collections.singletonList("frontend"))
            .setExpiration(null)
            .setSettingsMoniker(null));
  }

  @Test
  void targetedPlanValidatesDurationExpirationAndIdentity() {
    assertTargetedPlanInvalid(
        targetedPlan()
            .setRoles(Collections.singletonList("frontend"))
            .setImmediateProfilingDuration(361));
    assertTargetedPlanInvalid(
        targetedPlan()
            .setRoles(Collections.singletonList("frontend"))
            .setExpiration("not-a-timestamp"));

    TargetedCollectionPlanConfiguration plan =
        AlertConfigParser.toAlertingConfig(
                targetedConfiguration(
                    targetedPlan().setRoles(Collections.singletonList("frontend"))))
            .getTargetedCollectionPlanConfiguration();
    assertThat(plan).isNotNull();
    assertThat(
            AlertConfigParser.toAlertingConfig(
                    targetedConfiguration(
                        targetedPlan().setRoles(Collections.singletonList("frontend"))))
                .hasAnEnabledTrigger(null, "instance-1", Instant.parse("2099-01-01T00:00:00Z")))
        .isFalse();
  }

  private static void assertTargetedPlanInvalid(TargetedCollectionPlan plan) {
    TargetedCollectionPlanConfiguration parsedPlan =
        AlertConfigParser.toAlertingConfig(targetedConfiguration(plan))
            .getTargetedCollectionPlanConfiguration();
    assertThat(parsedPlan).isNotNull();
    assertThat(
            AlertConfigParser.toAlertingConfig(targetedConfiguration(plan))
                .hasAnEnabledTrigger(
                    "frontend", "instance-1", Instant.parse("2099-01-01T00:00:00Z")))
        .isFalse();
  }

  private static ProfilerConfiguration targetedConfiguration(TargetedCollectionPlan plan) {
    return new ProfilerConfiguration().setCollectionPlan("").setTargetedCollectionPlan(plan);
  }

  private static TargetedCollectionPlan targetedPlan() {
    return new TargetedCollectionPlan()
        .setImmediateProfilingDuration(120)
        .setExpiration("2099-08-17T19:00:00.0000000Z")
        .setSettingsMoniker("Portal_test");
  }
}
