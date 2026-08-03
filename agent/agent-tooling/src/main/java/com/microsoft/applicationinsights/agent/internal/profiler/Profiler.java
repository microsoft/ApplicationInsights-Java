// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadListener;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import io.opentelemetry.contrib.jfr.connection.FlightRecorderConnection;
import io.opentelemetry.contrib.jfr.connection.JfrConnectionException;
import io.opentelemetry.contrib.jfr.connection.Recording;
import io.opentelemetry.contrib.jfr.connection.RecordingConfiguration;
import io.opentelemetry.contrib.jfr.connection.RecordingOptions;
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
import javax.management.MBeanServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages connecting JFR interaction.
 *
 * <ul>
 *   <li>Instantiates FlightRecorder subsystem
 *   <li>Creates profiles on demand
 *   <li>Enforces a global cooldown between recordings to prevent rapid successive profiles from
 *       different trigger sources (CPU, memory, request, manual, periodic)
 * </ul>
 */
public class Profiler {

  private static final Logger logger = LoggerFactory.getLogger(Profiler.class);

  private static final Duration DEFAULT_CONTINUOUS_PROFILING_MAX_AGE = Duration.ofMinutes(2);

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

  // Global cooldown: earliest time at which a new recording is allowed, regardless of trigger type.
  // This prevents rapid successive profiles from different trigger sources (e.g., file trigger
  // immediately followed by a CPU threshold breach). Reset after each recording completes.
  private volatile Instant globalCooldownUntil = Instant.MIN;

  // Duration (in seconds) of the global cooldown period. A value of 0 disables the global cooldown
  // (individual per-trigger cooldowns still apply).
  private final int globalCooldownSeconds;

  private final RecordingConfiguration memoryRecordingConfiguration;
  private final RecordingConfiguration cpuRecordingConfiguration;
  private final RecordingConfiguration spanRecordingConfiguration;
  private final RecordingConfiguration manualRecordingConfiguration;

  private final boolean continuousProfilingEnabled;
  private final Duration continuousProfilingMaxAge;
  private final RecordingConfiguration continuousRecordingConfiguration;

  // Long-running recording backed by a circular buffer (maxAge, no duration) used when
  // continuous profiling is enabled. Guarded by activeRecordingLock.
  @Nullable private Recording continuousRecording = null;

  private final File temporaryDirectory;

  private final TimeSource timeSource;

  public Profiler(Configuration.ProfilerConfiguration config, File tempDir) {
    this(config, tempDir, TimeSource.DEFAULT);
  }

  public Profiler(Configuration.ProfilerConfiguration config, File tempDir, TimeSource timeSource) {

    this.timeSource = timeSource;

    periodicConfig =
        AlertConfiguration.builder()
            .setType(AlertMetricType.PERIODIC)
            .setEnabled(false)
            .setThreshold(0.0f)
            .setProfileDurationSeconds(config.periodicRecordingDurationSeconds)
            .setCooldownSeconds(config.periodicRecordingIntervalSeconds)
            .build();

    globalCooldownSeconds = config.globalCooldownSeconds;

    memoryRecordingConfiguration = AlternativeJfrConfigurations.getMemoryProfileConfig(config);
    cpuRecordingConfiguration = AlternativeJfrConfigurations.getCpuProfileConfig(config);
    spanRecordingConfiguration = AlternativeJfrConfigurations.getSpanProfileConfig(config);
    manualRecordingConfiguration = AlternativeJfrConfigurations.getManualProfileConfig(config);
    continuousProfilingEnabled = config.enableContinuousProfiling;
    continuousProfilingMaxAge =
        resolveContinuousProfilingMaxAge(config.continuousProfilingMaxAgeSeconds);
    // Continuous profiling uses a single always-on recording, so it can only carry one JFC. Reuse
    // the CPU configuration rather than opening a second stream on the same resource.
    continuousRecordingConfiguration = cpuRecordingConfiguration;
    temporaryDirectory = tempDir;
  }

  private static Duration resolveContinuousProfilingMaxAge(int continuousProfilingMaxAgeSeconds) {
    if (continuousProfilingMaxAgeSeconds <= 0) {
      logger.warn(
          "continuousProfilingMaxAgeSeconds must be positive but was {}; falling back to {}s",
          continuousProfilingMaxAgeSeconds,
          DEFAULT_CONTINUOUS_PROFILING_MAX_AGE.getSeconds());
      return DEFAULT_CONTINUOUS_PROFILING_MAX_AGE;
    }
    return Duration.ofSeconds(continuousProfilingMaxAgeSeconds);
  }

  /**
   * Call init before run.
   *
   * @throws IOException Trouble communicating with MBean server
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
    } catch (JfrConnectionException jfrConnectionException) {
      // Possibly an older JVM, try using Diagnostic command
      flightRecorderConnection = FlightRecorderConnection.diagnosticCommandConnection(mbeanServer);
    }

    startContinuousRecordingIfEnabled();
  }

  // visible for testing
  void initialize(
      UploadService uploadService,
      ScheduledExecutorService scheduledExecutorService,
      FlightRecorderConnection flightRecorderConnection) {
    this.uploadService = uploadService;
    this.scheduledExecutorService = scheduledExecutorService;
    this.recordingOptionsBuilder = new RecordingOptions.Builder();
    this.flightRecorderConnection = flightRecorderConnection;

    startContinuousRecordingIfEnabled();
  }

  /** Apply new configuration settings obtained from Service Profiler. */
  public void updateConfiguration(ProfilerConfiguration newConfig) {
    logger.debug("Received config {}", newConfig.getLastModified());

    // TODO update periodic profile configuration
  }

  // visible for tests
  void profileAndUpload(AlertBreach alertBreach, Duration duration, UploadListener uploadListener) {
    Instant recordingStart = timeSource.getNow();
    if (continuousProfilingEnabled) {
      captureContinuousRecording(alertBreach, duration, recordingStart, uploadListener);
      return;
    }
    executeProfile(
        alertBreach.getType(),
        duration,
        uploadNewRecording(alertBreach, recordingStart, uploadListener));
  }

  private void startContinuousRecordingIfEnabled() {
    if (!continuousProfilingEnabled) {
      return;
    }
    synchronized (activeRecordingLock) {
      if (continuousRecording != null) {
        return;
      }
      try {
        // A continuous recording uses a circular buffer bounded by maxAge and no duration, so it
        // runs indefinitely while only retaining the most recent window of data on disk.
        // Use a dedicated builder so the maxAge/disk options for the continuous circular buffer do
        // not mutate the shared recordingOptionsBuilder used by on-demand recordings.
        RecordingOptions recordingOptions =
            new RecordingOptions.Builder()
                .maxAge(continuousProfilingMaxAge.toMillis() + " ms")
                .disk("true")
                .build();
        continuousRecording = createRecording(recordingOptions, continuousRecordingConfiguration);
        continuousRecording.start();
        logger.info(
            "Started continuous JFR recording with circular buffer maxAge of {} seconds",
            continuousProfilingMaxAge.getSeconds());
      } catch (IOException | JfrConnectionException e) {
        logger.error("Failed to start continuous JFR recording", e);
        continuousRecording = null;
      }
    }
  }

  @SuppressWarnings(
      "CatchingUnchecked") // catching unchecked exception is necessary for proper error handling
  private void captureContinuousRecording(
      AlertBreach alertBreach,
      Duration requestedDuration,
      Instant recordingEnd,
      UploadListener uploadListener) {
    File dumpFile;
    Instant bufferStart;
    synchronized (activeRecordingLock) {
      if (continuousRecording == null) {
        logger.warn("Profile requested but continuous recording is not running, ignoring request.");
        return;
      }

      // Enforce global cooldown across all trigger sources
      if (globalCooldownSeconds > 0 && timeSource.getNow().isBefore(globalCooldownUntil)) {
        logger.info(
            "Profile requested (type={}), but global cooldown is active until {}. Ignoring request.",
            alertBreach.getType(),
            globalCooldownUntil);
        return;
      }

      // The circular buffer only retains up to maxAge of data, so the captured window can never
      // exceed maxAge. Honor a shorter portal-/JMX-configured profile duration when one is
      // provided, otherwise fall back to the full maxAge window.
      Duration captureWindow = resolveContinuousCaptureWindow(requestedDuration);

      // The dumped buffer covers [recordingEnd - captureWindow, recordingEnd]. Use the start of
      // that
      // window as the profile timestamp and file name so the profile is indexed at the point the
      // data actually begins rather than at dump time.
      bufferStart = recordingEnd.minus(captureWindow);

      try {
        dumpFile = createJfrFile(bufferStart, recordingEnd);
      } catch (IOException e) {
        logger.error("Failed to create jfr file", e);
        return;
      }

      try {
        // Dump the trailing captureWindow of the circular buffer. The continuous recording keeps
        // running so future requests can be serviced immediately.
        dumpContinuousRecording(dumpFile, recordingEnd, captureWindow);
      } catch (IOException | JfrConnectionException e) {
        logger.error("Failed to dump continuous recording", e);
        if (dumpFile.exists() && !dumpFile.delete()) {
          logger.error("Failed to remove file " + dumpFile.getAbsolutePath());
        }
        return;
      }

      // Start the global cooldown while still holding the lock so concurrent triggers are rejected
      // before they can dump another snapshot.
      startGlobalCooldown();
    }

    try {
      logger.info("Uploading continuous recording snapshot");
      uploadService.upload(alertBreach, bufferStart.toEpochMilli(), dumpFile, uploadListener);
    } catch (Exception e) {
      logger.error("Failed to upload recording", e);
    } catch (Error e) {
      // rethrow errors
      logger.error("Failed to upload recording", e);
      throw e;
    } finally {
      if (dumpFile.exists() && !dumpFile.delete()) {
        logger.error("Failed to remove file " + dumpFile.getAbsolutePath());
      }
    }
  }

  /**
   * Resolves the window of buffered data to capture from the continuous recording. The window is
   * bounded by the configured continuous profiling maxAge (the circular buffer can hold no more
   * than that) but is otherwise driven by the requested profile duration so that a shorter
   * portal-/JMX-configured duration is honored rather than silently ignored.
   */
  private Duration resolveContinuousCaptureWindow(@Nullable Duration requestedDuration) {
    if (requestedDuration == null
        || requestedDuration.isZero()
        || requestedDuration.isNegative()
        || requestedDuration.compareTo(continuousProfilingMaxAge) > 0) {
      return continuousProfilingMaxAge;
    }
    return requestedDuration;
  }

  /**
   * Writes the trailing {@code captureWindow} of the continuous recording's circular buffer to
   * {@code dumpFile}. When the requested window covers the whole buffer the more robust {@link
   * Recording#dump(String)} path is used; otherwise only the requested trailing window is streamed
   * out so the configured profile duration is respected.
   */
  private void dumpContinuousRecording(File dumpFile, Instant recordingEnd, Duration captureWindow)
      throws IOException, JfrConnectionException {
    if (captureWindow.compareTo(continuousProfilingMaxAge) >= 0) {
      continuousRecording.dump(dumpFile.getAbsolutePath());
      return;
    }
    writeFileFromStream(
        continuousRecording, dumpFile, recordingEnd.minus(captureWindow), recordingEnd);
  }

  @Nullable
  private Recording startRecording(AlertMetricType alertType, Duration duration) {
    synchronized (activeRecordingLock) {
      if (activeRecording != null) {
        logger.warn("Alert received, however a profile is already in progress, ignoring request.");
        return null;
      }

      // Enforce global cooldown across all trigger sources
      if (globalCooldownSeconds > 0 && timeSource.getNow().isBefore(globalCooldownUntil)) {
        logger.info(
            "Alert received (type={}), but global cooldown is active until {}. Ignoring request.",
            alertType,
            globalCooldownUntil);
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
    } catch (JfrConnectionException internalError) {
      logger.error("Internal JFR Error", internalError);
      CompletableFuture<?> future = new CompletableFuture<>();
      future.completeExceptionally(internalError);
    }
  }

  /** When a profile has been created, upload it to service profiler. */
  @SuppressWarnings(
      "CatchingUnchecked") // catching unchecked exception is necessary for proper error handling
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
    } catch (JfrConnectionException internalError) {
      // Sometimes the  mbean dump fails...Try alternative of streaming data out
      try {
        writeFileFromStream(recording, recordingFile);
      } catch (IOException e) {
        logger.error("Failed to close recording", e);
      } catch (JfrConnectionException e) {
        logger.error("Internal JFR Error", e);
      }
    } finally {
      try {
        recording.close();
      } catch (IOException | JfrConnectionException e) {
        logger.error("Failed to close recording", e);
      }
    }
  }

  private static void writeFileFromStream(Recording recording, File recordingFile)
      throws IOException, JfrConnectionException {
    writeFileFromStream(recording, recordingFile, null, null);
  }

  private static void writeFileFromStream(
      Recording recording, File recordingFile, @Nullable Instant start, @Nullable Instant end)
      throws IOException, JfrConnectionException {
    if (recordingFile.exists()) {
      recordingFile.delete();
    }
    recordingFile.createNewFile();

    try (BufferedInputStream stream = new BufferedInputStream(recording.getStream(start, end));
        FileOutputStream fos = new FileOutputStream(recordingFile)) {
      int read;
      byte[] buffer = new byte[10 * 1024];
      while ((read = stream.read(buffer)) != -1) {
        fos.write(buffer, 0, read);
      }
    }
  }

  // visible for testing
  void clearActiveRecording() {
    synchronized (activeRecordingLock) {
      activeRecording = null;

      // Start global cooldown now that the recording is complete
      startGlobalCooldown();

      // delete uploaded profile
      if (activeRecordingFile != null && activeRecordingFile.exists()) {
        if (!activeRecordingFile.delete()) {
          logger.error("Failed to remove file " + activeRecordingFile.getAbsolutePath());
        }
      }
      activeRecordingFile = null;
    }
  }

  // Advances the global cooldown window. Callers must hold activeRecordingLock.
  private void startGlobalCooldown() {
    if (globalCooldownSeconds > 0) {
      globalCooldownUntil = timeSource.getNow().plusSeconds(globalCooldownSeconds);
      logger.debug("Global profile cooldown active until {}", globalCooldownUntil);
    }
  }

  // visible for testing
  Instant getGlobalCooldownUntil() {
    return globalCooldownUntil;
  }

  // visible for testing
  boolean isRecordingActive() {
    synchronized (activeRecordingLock) {
      return activeRecording != null;
    }
  }

  /** Dump JFR profile to file. */
  // visible for testing
  protected File createJfrFile(Duration duration) throws IOException {
    Instant recordingStart = timeSource.getNow();
    return createJfrFile(recordingStart, recordingStart.plus(duration));
  }

  /** Create a JFR file whose name encodes the window of data it contains. */
  // visible for testing
  protected File createJfrFile(Instant recordingStart, Instant recordingEnd) throws IOException {
    if (!temporaryDirectory.exists()) {
      if (!temporaryDirectory.mkdirs()) {
        throw new IOException(
            "Failed to create temporary directory " + temporaryDirectory.getAbsolutePath());
      }
    }

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
