// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingProfileFileTriggerConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlertingSubsystemFileTriggerTest {

  @TempDir File tempDir;

  private static TestTimeSource createTimeSourceAtFileModification(File triggerFile) {
    TestTimeSource timeSource = new TestTimeSource();
    timeSource.setNow(Instant.ofEpochMilli(triggerFile.lastModified()));
    return timeSource;
  }

  private static AlertingConfiguration defaultAlertingConfig() {
    return AlertingConfiguration.create(
        AlertConfiguration.builder()
            .setType(AlertMetricType.CPU)
            .setEnabled(false)
            .setThreshold(80.0f)
            .setProfileDurationSeconds(30)
            .setCooldownSeconds(14400)
            .build(),
        AlertConfiguration.builder()
            .setType(AlertMetricType.MEMORY)
            .setEnabled(false)
            .setThreshold(20.0f)
            .setProfileDurationSeconds(120)
            .setCooldownSeconds(14400)
            .build(),
        DefaultConfiguration.builder().build(),
        CollectionPlanConfiguration.builder()
            .setSingle(false)
            .setMode(EngineMode.immediate)
            .setExpiration(Instant.now())
            .setImmediateProfilingDurationSeconds(0)
            .setSettingsMoniker("")
            .build(),
        new ArrayList<>());
  }

  @Test
  void fileTriggerFiresWhenFileExistsAndIsRecent() throws IOException {
    File triggerFile = new File(tempDir, "trigger");
    assertThat(triggerFile.createNewFile()).isTrue();

    AtomicReference<AlertBreach> breach = new AtomicReference<>();
    Consumer<AlertBreach> consumer = breach::set;

    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(true, "trigger", 120, tempDir);

    AlertingSubsystem subsystem =
        AlertingSubsystem.create(consumer, createTimeSourceAtFileModification(triggerFile), config);

    subsystem.updateConfiguration(defaultAlertingConfig());

    assertThat(breach.get()).isNotNull();
    assertThat(breach.get().getType()).isEqualTo(AlertMetricType.MANUAL);
    assertThat(breach.get().getAlertConfiguration().getProfileDurationSeconds()).isEqualTo(120);
    // trigger file should be deleted
    assertThat(triggerFile.exists()).isFalse();
  }

  @Test
  void fileTriggerDoesNotFireWhenDisabled() throws IOException {
    File triggerFile = new File(tempDir, "trigger");
    assertThat(triggerFile.createNewFile()).isTrue();

    AtomicReference<AlertBreach> breach = new AtomicReference<>();
    Consumer<AlertBreach> consumer = breach::set;

    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(false, "trigger", 120, tempDir);

    AlertingSubsystem subsystem = AlertingSubsystem.create(consumer, new TestTimeSource(), config);

    subsystem.updateConfiguration(defaultAlertingConfig());

    assertThat(breach.get()).isNull();
    // file should not be deleted when disabled
    assertThat(triggerFile.exists()).isTrue();
  }

  @Test
  void fileTriggerDoesNotFireWhenFileDoesNotExist() {
    AtomicReference<AlertBreach> breach = new AtomicReference<>();
    Consumer<AlertBreach> consumer = breach::set;

    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(true, "trigger", 120, tempDir);

    AlertingSubsystem subsystem = AlertingSubsystem.create(consumer, new TestTimeSource(), config);

    subsystem.updateConfiguration(defaultAlertingConfig());

    assertThat(breach.get()).isNull();
  }

  @Test
  void fileTriggerDoesNotFireWhenFileIsTooOld() throws IOException {
    File triggerFile = new File(tempDir, "trigger");
    assertThat(triggerFile.createNewFile()).isTrue();

    // Use a time source with a known "current" time, and set the file's lastModified to be
    // older than the max age relative to that time.
    long currentTimeMillis = System.currentTimeMillis();
    long oldLastModified =
        currentTimeMillis
            - AlertingProfileFileTriggerConfiguration.MANUAL_TRIGGER_FILE_MAX_AGE_MS
            - 10_000;
    assertThat(triggerFile.setLastModified(oldLastModified)).isTrue();

    TestTimeSource timeSource = new TestTimeSource();
    timeSource.setNow(Instant.ofEpochMilli(currentTimeMillis));

    AtomicReference<AlertBreach> breach = new AtomicReference<>();
    Consumer<AlertBreach> consumer = breach::set;

    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(true, "trigger", 120, tempDir);

    AlertingSubsystem subsystem = AlertingSubsystem.create(consumer, timeSource, config);

    subsystem.updateConfiguration(defaultAlertingConfig());

    assertThat(breach.get()).isNull();
    // file should not be deleted when too old
    assertThat(triggerFile.exists()).isTrue();
  }

  @Test
  void fileTriggerDeletesFileOnSuccessfulTrigger() throws IOException {
    File triggerFile = new File(tempDir, "trigger");
    assertThat(triggerFile.createNewFile()).isTrue();

    AtomicReference<AlertBreach> breach = new AtomicReference<>();
    Consumer<AlertBreach> consumer = breach::set;

    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(true, "trigger", 120, tempDir);

    AlertingSubsystem subsystem =
        AlertingSubsystem.create(consumer, createTimeSourceAtFileModification(triggerFile), config);

    subsystem.updateConfiguration(defaultAlertingConfig());

    assertThat(breach.get()).isNotNull();
    assertThat(triggerFile.exists()).isFalse();
  }

  @Test
  void fileTriggerUsesDefaultDurationWhenCollectionPlanHasZero() throws IOException {
    File triggerFile = new File(tempDir, "trigger");
    assertThat(triggerFile.createNewFile()).isTrue();

    AtomicReference<AlertBreach> breach = new AtomicReference<>();
    Consumer<AlertBreach> consumer = breach::set;

    int expectedDefaultDuration = 90;
    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(
            true, "trigger", expectedDefaultDuration, tempDir);

    AlertingSubsystem subsystem =
        AlertingSubsystem.create(consumer, createTimeSourceAtFileModification(triggerFile), config);

    // The default alerting config has immediateProfilingDurationSeconds=0
    subsystem.updateConfiguration(defaultAlertingConfig());

    assertThat(breach.get()).isNotNull();
    assertThat(breach.get().getAlertConfiguration().getProfileDurationSeconds())
        .isEqualTo(expectedDefaultDuration);
  }

  @Test
  void fileTriggerDoesNotFireWhenFileTimestampIsInFuture() throws IOException {
    File triggerFile = new File(tempDir, "trigger");
    assertThat(triggerFile.createNewFile()).isTrue();

    TestTimeSource timeSource = new TestTimeSource();
    long currentMillis = System.currentTimeMillis();
    timeSource.setNow(Instant.ofEpochMilli(currentMillis));
    assertThat(triggerFile.setLastModified(currentMillis + 1_000)).isTrue();

    AtomicReference<AlertBreach> breach = new AtomicReference<>();
    Consumer<AlertBreach> consumer = breach::set;

    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(true, "trigger", 120, tempDir);

    AlertingSubsystem subsystem = AlertingSubsystem.create(consumer, timeSource, config);

    subsystem.updateConfiguration(defaultAlertingConfig());

    assertThat(breach.get()).isNull();
    assertThat(triggerFile.exists()).isTrue();
  }

  @Test
  void fileTriggerConfigurationFallsBackToDefaultPathWhenConfiguredPathIsNull() {
    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(true, null, 120, tempDir);

    assertThat(config.getFilePath().getAbsolutePath())
        .isEqualTo(new File(tempDir, "applicationinsights-agent-profile-trigger").getAbsolutePath());
  }

  @Test
  void fileTriggerConfigurationAllowsNullTempDirForRelativePath() {
    AlertingProfileFileTriggerConfiguration config =
        AlertingProfileFileTriggerConfiguration.create(true, "trigger", 120, null);

    assertThat(config.getFilePath().getPath()).isEqualTo("trigger");
  }
}
