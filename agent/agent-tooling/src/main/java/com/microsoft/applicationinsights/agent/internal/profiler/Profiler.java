// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadListener;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.jfr.FlightRecorderConnection;
import com.microsoft.jfr.JfrStreamingException;
import com.microsoft.jfr.Recording;
import com.microsoft.jfr.RecordingConfiguration;
import com.microsoft.jfr.RecordingOptions;
import com.microsoft.jfr.dcmd.FlightRecorderDiagnosticCommandConnection;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages connecting JFR interaction.
 *
 * <ul>
 *   <li>Instantiates FlightRecorder subsystem
 *   <li>Creates profiles on demand
 * </ul>
 */
public class Profiler {

  private static final Logger logger = LoggerFactory.getLogger(Profiler.class);

  // service execution context
  private ScheduledExecutorService scheduledExecutorService;

  // Action to perform when a profile has been created
  private UploadService uploadService;

  private FlightRecorderConnection flightRecorderConnection;
  private RecordingOptions.Builder recordingOptionsBuilder;

  private final AlertConfiguration periodicConfig;

  private final Object activeRecordingLock = new Object();
  @Nullable private Recording activeRecording = null;
  @Nullable private File activeRecordingFile = null;

  private final RecordingConfiguration memoryRecordingConfiguration;
  private final RecordingConfiguration cpuRecordingConfiguration;
  private final RecordingConfiguration spanRecordingConfiguration;
  private final RecordingConfiguration manualRecordingConfiguration;

  private final File temporaryDirectory;

  public Profiler(Configuration.ProfilerConfiguration config, File tempDir) {

    periodicConfig =
        AlertConfiguration.builder()
            .setType(AlertMetricType.PERIODIC)
            .setEnabled(false)
            .setThreshold(0.0f)
            .setProfileDurationSeconds(config.periodicRecordingDurationSeconds)
            .setCooldownSeconds(config.periodicRecordingIntervalSeconds)
            .build();

    memoryRecordingConfiguration = AlternativeJfrConfigurations.getMemoryProfileConfig(config);
    cpuRecordingConfiguration = AlternativeJfrConfigurations.getCpuProfileConfig(config);
    spanRecordingConfiguration = AlternativeJfrConfigurations.getSpanProfileConfig(config);
    manualRecordingConfiguration = AlternativeJfrConfigurations.getManualProfileConfig(config);
    temporaryDirectory = tempDir;
  }

  /**
   * Call init before run.
   *
   * @throws IOException Trouble communicating with MBean server
   * @throws InstanceNotFoundException The JVM does not support JFR, or experimental option is not
   *     enabled.
   */
  public void initialize(
      UploadService uploadService, ScheduledExecutorService scheduledExecutorService)
      throws Exception {
    this.uploadService = uploadService;
    this.scheduledExecutorService = scheduledExecutorService;

    // TODO -  allow user configuration of profile options
    recordingOptionsBuilder = new RecordingOptions.Builder();

    // connect to mbeans
    MBeanServerConnection mbeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      flightRecorderConnection = FlightRecorderConnection.connect(mbeanServer);
    } catch (JfrStreamingException | InstanceNotFoundException jfrStreamingException) {
      // Possibly an older JVM, try using Diagnostic command
      flightRecorderConnection = FlightRecorderDiagnosticCommandConnection.connect(mbeanServer);
    }
  }

  /** Apply new configuration settings obtained from Service Profiler. */
  public void updateConfiguration(ProfilerConfiguration newConfig) {
    logger.debug("Received config {}", newConfig.getLastModified());

    // TODO update periodic profile configuration
  }

  // visible for tests
  void profileAndUpload(AlertBreach alertBreach, Duration duration, UploadListener uploadListener) {
    Instant recordingStart = Instant.now();
    executeProfile(
        alertBreach.getType(),
        duration,
        uploadNewRecording(alertBreach, recordingStart, uploadListener));
  }

  @Nullable
  private Recording startRecording(AlertMetricType alertType, Duration duration) {
    synchronized (activeRecordingLock) {
      if (activeRecording != null) {
        logger.warn("Alert received, however a profile is already in progress, ignoring request.");
        return null;
      }

      RecordingConfiguration recordingConfiguration;
      switch (alertType) {
        case REQUEST:
          recordingConfiguration = spanRecordingConfiguration;
          break;
        case MEMORY:
          recordingConfiguration = memoryRecordingConfiguration;
          break;
        case MANUAL:
          recordingConfiguration = manualRecordingConfiguration;
          break;
        default:
          recordingConfiguration = cpuRecordingConfiguration;
          break;
      }

      try {
        activeRecordingFile = createJfrFile(duration);

        // As a fallback in case recording closing logic does not succeed, set the recording
        // duration to the expected duration plus 60 seconds
        Duration requestedDuration = duration.plus(60, ChronoUnit.SECONDS);

        RecordingOptions recordingOptions =
            recordingOptionsBuilder.duration(requestedDuration.toMillis() + " ms").build();

        this.activeRecording = createRecording(recordingOptions, recordingConfiguration);

        return activeRecording;
      } catch (IOException e) {
        logger.error("Failed to create jfr file", e);
        return null;
      }
    }
  }

  // visible for tests
  protected Recording createRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration) {
    return flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration);
  }

  /** Perform a profile and notify the handler. */
  private void executeProfile(
      AlertMetricType alertType, Duration duration, Consumer<Recording> handler) {

    logger.info("Received " + alertType + " alert, Starting profile");

    if (flightRecorderConnection == null) {
      logger.error("Flight recorder not initialised");
      return;
    }

    Recording newRecording = startRecording(alertType, duration);

    if (newRecording == null) {
      return;
    }

    try {
      newRecording.start();

      // schedule closing the recording
      scheduledExecutorService.schedule(
          () -> handler.accept(newRecording), duration.getSeconds(), TimeUnit.SECONDS);

    } catch (IOException ioException) {
      logger.error("Failed to start JFR recording", ioException);
      CompletableFuture<?> future = new CompletableFuture<>();
      future.completeExceptionally(ioException);
    } catch (JfrStreamingException internalError) {
      logger.error("Internal JFR Error", internalError);
      CompletableFuture<?> future = new CompletableFuture<>();
      future.completeExceptionally(internalError);
    }
  }

  /** When a profile has been created, upload it to service profiler. */
  @SuppressWarnings("CatchingUnchecked")
  private Consumer<Recording> uploadNewRecording(
      AlertBreach alertBreach, Instant recordingStart, UploadListener uploadListener) {
    return recording -> {
      logger.info("Closing and uploading recording");
      try {
        // dump profile to file
        closeRecording(activeRecording, activeRecordingFile);

        // upload new profile
        uploadService.upload(
            alertBreach, recordingStart.toEpochMilli(), activeRecordingFile, uploadListener);

      } catch (Exception e) {
        logger.error("Failed to upload recording", e);
      } catch (Error e) {
        // rethrow errors
        logger.error("Failed to upload recording", e);
        throw e;
      } finally {
        clearActiveRecording();
      }
    };
  }

  private static void closeRecording(Recording recording, File recordingFile) {
    try {
      // close recording
      recording.dump(recordingFile.getAbsolutePath());
    } catch (IOException e) {
      logger.error("Failed to close recording", e);
    } catch (JfrStreamingException internalError) {
      // Sometimes the  mbean dump fails...Try alternative of streaming data out
      try {
        writeFileFromStream(recording, recordingFile);
      } catch (IOException e) {
        logger.error("Failed to close recording", e);
      } catch (JfrStreamingException e) {
        logger.error("Internal JFR Error", e);
      }
    } finally {
      try {
        recording.close();
      } catch (IOException e) {
        logger.error("Failed to close recording", e);
      }
    }
  }

  private static void writeFileFromStream(Recording recording, File recordingFile)
      throws IOException, JfrStreamingException {
    if (recordingFile.exists()) {
      recordingFile.delete();
    }
    recordingFile.createNewFile();

    try (BufferedInputStream stream = new BufferedInputStream(recording.getStream(null, null));
        FileOutputStream fos = new FileOutputStream(recordingFile)) {
      int read;
      byte[] buffer = new byte[10 * 1024];
      while ((read = stream.read(buffer)) != -1) {
        fos.write(buffer, 0, read);
      }
    }
  }

  private void clearActiveRecording() {
    synchronized (activeRecordingLock) {
      activeRecording = null;

      // delete uploaded profile
      if (activeRecordingFile != null && activeRecordingFile.exists()) {
        if (!activeRecordingFile.delete()) {
          logger.error("Failed to remove file " + activeRecordingFile.getAbsolutePath());
        }
      }
      activeRecordingFile = null;
    }
  }

  /** Dump JFR profile to file. */
  // visible for testing
  protected File createJfrFile(Duration duration) throws IOException {
    if (!temporaryDirectory.exists()) {
      if (!temporaryDirectory.mkdirs()) {
        throw new IOException(
            "Failed to create temporary directory " + temporaryDirectory.getAbsolutePath());
      }
    }

    Instant recordingStart = Instant.now();
    Instant recordingEnd = recordingStart.plus(duration);

    return new File(
        temporaryDirectory,
        "recording_" + recordingStart.toEpochMilli() + "-" + recordingEnd.toEpochMilli() + ".jfr");
  }

  /** Action to be performed on a periodic profile request. */
  private void performPeriodicProfile(UploadListener uploadListener) {
    logger.info("Received periodic profile request");

    AlertBreach breach =
        AlertBreach.builder()
            .setType(AlertMetricType.PERIODIC)
            .setAlertValue(0)
            .setAlertConfiguration(periodicConfig)
            .setProfileId(UUID.randomUUID().toString())
            .build();
    profileAndUpload(
        breach,
        Duration.ofSeconds(breach.getAlertConfiguration().getProfileDurationSeconds()),
        uploadListener);
  }

  /** Dispatch alert breach event to handler. */
  // visible for tests
  public void accept(AlertBreach alertBreach, UploadListener uploadListener) {

    if (alertBreach.getType() == AlertMetricType.PERIODIC) {
      performPeriodicProfile(uploadListener);
    } else {
      profileAndUpload(
          alertBreach,
          Duration.ofSeconds(alertBreach.getAlertConfiguration().getProfileDurationSeconds()),
          uploadListener);
    }
  }
}
