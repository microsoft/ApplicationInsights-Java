// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;

/**
 * Configuration for the file-based manual profile trigger.
 *
 * <p>When enabled, the alerting subsystem periodically checks for the existence of a trigger file.
 * If the file exists and was recently modified (within {@link #MANUAL_TRIGGER_FILE_MAX_AGE_MS}), it
 * is deleted and a manual profile recording is initiated.
 *
 * <p>This provides an operator-friendly mechanism for triggering on-demand profiles without
 * requiring JMX access – simply {@code touch} the trigger file from a shell or orchestration tool.
 */
public class AlertingProfileFileTriggerConfiguration {

  /**
   * Maximum age (in milliseconds) of the trigger file for it to be considered valid. Files older
   * than this threshold are ignored to prevent stale trigger files from initiating unexpected
   * recordings (e.g., after a restart).
   */
  public static final long MANUAL_TRIGGER_FILE_MAX_AGE_MS = 60_000; // 1 minute

  // Whether the file-based manual trigger is enabled.
  private final boolean enabled;

  // Path to the file that triggers a manual profile when created/touched.
  // If relative, it is resolved against the agent's temp directory.
  private final File filePath;

  /** The default duration (in seconds) used for profiles triggered via this file mechanism. */
  private final int defaultProfileDurationSeconds;

  private AlertingProfileFileTriggerConfiguration(
      boolean enabled, File filePath, int defaultProfileDurationSeconds) {
    this.enabled = enabled;
    this.filePath = filePath;
    this.defaultProfileDurationSeconds = defaultProfileDurationSeconds;
  }

  /**
   * Creates a file trigger configuration, resolving relative paths against the provided temp
   * directory.
   *
   * @param enabled whether file-based triggering is active
   * @param filePath path to the trigger file (absolute or relative to {@code tempDir})
   * @param defaultProfileDurationSeconds duration in seconds for the profile if no override is
   *     configured in the collection plan
   * @param tempDir base directory used to resolve relative file paths
   * @return a fully resolved configuration instance
   */
  @SuppressFBWarnings(
      value = "SECPTI",
      justification = "File path is set by trusted user configuration (applicationinsights.json)")
  public static AlertingProfileFileTriggerConfiguration create(
      boolean enabled, String filePath, int defaultProfileDurationSeconds, File tempDir) {

    File manualTriggerFile = new File(filePath);
    if (!manualTriggerFile.isAbsolute()) {
      manualTriggerFile = new File(tempDir, filePath);
    }

    return new AlertingProfileFileTriggerConfiguration(
        enabled, manualTriggerFile, defaultProfileDurationSeconds);
  }

  /** Creates a disabled configuration suitable for tests. */
  public static AlertingProfileFileTriggerConfiguration createDefault() {
    return new AlertingProfileFileTriggerConfiguration(
        false, new File("applicationinsights-agent-profile-trigger"), 120);
  }

  /** Returns the default profile duration in seconds for file-triggered recordings. */
  public int getDefaultProfileDurationSeconds() {
    return defaultProfileDurationSeconds;
  }

  /** Returns the resolved path of the trigger file. */
  public File getFilePath() {
    return filePath;
  }

  /** Returns whether the file-based manual trigger is enabled. */
  public boolean isEnabled() {
    return enabled;
  }
}
