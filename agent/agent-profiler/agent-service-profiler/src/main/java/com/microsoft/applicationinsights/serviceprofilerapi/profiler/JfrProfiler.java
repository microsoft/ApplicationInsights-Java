/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.serviceprofilerapi.profiler;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import com.microsoft.applicationinsights.profiler.ProfileHandler;
import com.microsoft.applicationinsights.profiler.Profiler;
import com.microsoft.applicationinsights.profiler.ProfilerConfiguration;
import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.jfr.FlightRecorderConnection;
import com.microsoft.jfr.JfrStreamingException;
import com.microsoft.jfr.Recording;
import com.microsoft.jfr.RecordingConfiguration;
import com.microsoft.jfr.RecordingOptions;
import com.microsoft.jfr.dcmd.FlightRecorderDiagnosticCommandConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import org.checkerframework.checker.nullness.qual.Nullable;
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
public class JfrProfiler implements ProfilerConfigurationHandler, Profiler {
  private static final Logger LOGGER = LoggerFactory.getLogger(JfrProfiler.class);
  public static final String REDUCED_MEMORY_PROFILE = "reduced-memory-profile.jfc";
  public static final String REDUCED_CPU_PROFILE = "reduced-cpu-profile.jfc";

  // service execution context
  private ScheduledExecutorService scheduledExecutorService;

  // Action to perform when a profile has been created
  private ProfileHandler profileHandler;

  private FlightRecorderConnection flightRecorderConnection;
  private RecordingOptions recordingOptions;

  private final AlertConfiguration periodicConfig;

  private final Object activeRecordingLock = new Object();
  private Recording activeRecording = null;

  private final RecordingConfiguration memoryRecordingConfiguration;
  private final RecordingConfiguration cpuRecordingConfiguration;

  private final File temporaryDirectory;

  public JfrProfiler(ServiceProfilerServiceConfig configuration) {
    periodicConfig =
        new AlertConfiguration(
            AlertMetricType.PERIODIC,
            false,
            0.0f,
            configuration.getPeriodicRecordingDuration(),
            configuration.getPeriodicRecordingInterval());

    memoryRecordingConfiguration = getMemoryProfileConfig(configuration);
    cpuRecordingConfiguration = getCpuProfileConfig(configuration);
    temporaryDirectory = configuration.tempDirectory();
  }

  private static RecordingConfiguration getMemoryProfileConfig(
      ServiceProfilerServiceConfig configuration) {
    return getRecordingConfiguration(
        configuration.memoryTriggeredSettings(), REDUCED_MEMORY_PROFILE);
  }

  private static RecordingConfiguration getCpuProfileConfig(
      ServiceProfilerServiceConfig configuration) {
    return getRecordingConfiguration(configuration.cpuTriggeredSettings(), REDUCED_CPU_PROFILE);
  }

  private static RecordingConfiguration getRecordingConfiguration(
      String triggeredSettings, String reducedProfile) {
    if (triggeredSettings != null) {
      try {
        ProfileTypes profile = ProfileTypes.valueOf(triggeredSettings);

        if (profile == ProfileTypes.profile) {
          return RecordingConfiguration.PROFILE_CONFIGURATION;
        } else if (profile == ProfileTypes.profile_without_env_data) {
          return new RecordingConfiguration.JfcFileConfiguration(
              JfrProfiler.class.getResourceAsStream(reducedProfile));
        }
      } catch (IllegalArgumentException e) {
        // NOP, to be expected if a file is provided
      }

      try {
        FileInputStream fis = new FileInputStream(triggeredSettings);
        return new RecordingConfiguration.JfcFileConfiguration(fis);
      } catch (FileNotFoundException e) {
        LOGGER.error(
            "Failed to find custom JFC file " + triggeredSettings + " using default profile");
      }
    }

    return RecordingConfiguration.PROFILE_CONFIGURATION;
  }

  /**
   * Call init before run.
   *
   * @throws IOException Trouble communicating with MBean server
   * @throws InstanceNotFoundException The JVM does not support JFR, or experimental option is not
   *     enabled.
   */
  @Override
  public boolean initialize(
      ProfileHandler profileHandler, ScheduledExecutorService scheduledExecutorService)
      throws IOException, InstanceNotFoundException {
    this.profileHandler = profileHandler;
    this.scheduledExecutorService = scheduledExecutorService;

    // TODO -  allow user configuration of profile options
    recordingOptions = new RecordingOptions.Builder().build();

    try {
      // connect to mbeans
      MBeanServerConnection mbeanServer = ManagementFactory.getPlatformMBeanServer();
      try {
        flightRecorderConnection = FlightRecorderConnection.connect(mbeanServer);
      } catch (JfrStreamingException | InstanceNotFoundException jfrStreamingException) {
        // Possibly an older JVM, try using Diagnostic command
        flightRecorderConnection = FlightRecorderDiagnosticCommandConnection.connect(mbeanServer);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to connect to mbean", e);
      return false;
    }

    return true;
  }

  /** Apply new configuration settings obtained from Service Profiler. */
  @Override
  public void updateConfiguration(ProfilerConfiguration newConfig) {
    LOGGER.debug("Received config {}", newConfig.getLastModified());

    // TODO update periodic profile configuration
  }

  protected void profileAndUpload(AlertBreach alertBreach, Duration duration) {
    Instant recordingStart = Instant.now();
    executeProfile(
        alertBreach.getType(), duration, uploadNewRecording(alertBreach, recordingStart));
  }

  @Nullable
  private Recording startRecording(AlertMetricType alertType) {
    synchronized (activeRecordingLock) {
      if (activeRecording != null) {
        LOGGER.warn("Alert received, however a profile is already in progress, ignoring request.");
        return null;
      }

      RecordingConfiguration recordingConfiguration;
      switch (alertType) {
        case CPU:
          recordingConfiguration = cpuRecordingConfiguration;
          break;
        case MEMORY:
          recordingConfiguration = memoryRecordingConfiguration;
          break;
        default:
          recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;
      }

      activeRecording =
          flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration);
      return activeRecording;
    }
  }

  /** Perform a profile and notify the handler. */
  protected void executeProfile(
      AlertMetricType alertType, Duration duration, Consumer<Recording> handler) {

    LOGGER.info("Starting profile");

    if (flightRecorderConnection == null) {
      LOGGER.error("Flight recorder not initialised");
      return;
    }

    Recording newRecording = startRecording(alertType);

    if (newRecording == null) {
      return;
    }

    try {
      newRecording.start();

      // schedule closing the recording
      scheduledExecutorService.schedule(
          () -> handler.accept(newRecording), duration.getSeconds(), TimeUnit.SECONDS);

    } catch (IOException ioException) {
      LOGGER.error("Failed to start JFR recording", ioException);
      CompletableFuture<?> future = new CompletableFuture<>();
      future.completeExceptionally(ioException);
    } catch (JfrStreamingException internalError) {
      LOGGER.error("Internal JFR Error", internalError);
      CompletableFuture<?> future = new CompletableFuture<>();
      future.completeExceptionally(internalError);
    }
  }

  /** When a profile has been created, upload it to service profiler. */
  protected Consumer<Recording> uploadNewRecording(
      AlertBreach alertBreach, Instant recordingStart) {
    return recording -> {
      LOGGER.info("Closing and uploading recording");
      File file = null;
      try {
        Instant recordingEnd = Instant.now();

        // dump profile to file
        file = createJfrFile(recording, recordingStart, recordingEnd);

        // notify handler of a new profile
        profileHandler.receive(alertBreach, recordingStart.toEpochMilli(), file);

      } catch (Exception e) {
        LOGGER.error("Failed to upload recording", e);
      } catch (Error e) {
        // rethrow errors
        LOGGER.error("Failed to upload recording", e);
        throw e;
      } finally {
        try {
          // close recording
          recording.stop();
          recording.close();
        } catch (IOException e) {
          LOGGER.error("Failed to close recording", e);
        } catch (JfrStreamingException internalError) {
          LOGGER.error("Internal JFR Error", internalError);
        }

        // delete uploaded profile
        if (file != null) {
          if (file.exists()) {
            if (!file.delete()) {
              LOGGER.error("Failed to remove file " + file.getAbsolutePath());
            }
          }
        }

        clearActiveRecording();
      }
    };
  }

  private void clearActiveRecording() {
    synchronized (activeRecordingLock) {
      activeRecording = null;
    }
  }

  /** Dump JFR profile to file. */
  protected File createJfrFile(Recording recording, Instant recordingStart, Instant recordingEnd)
      throws IOException {
    try {
      if (!temporaryDirectory.exists()) {
        if (!temporaryDirectory.mkdirs()) {
          throw new IOException(
              "Failed to create temporary directory " + temporaryDirectory.getAbsolutePath());
        }
      }

      File file =
          new File(
              temporaryDirectory,
              "recording_"
                  + recordingStart.toEpochMilli()
                  + "-"
                  + recordingEnd.toEpochMilli()
                  + ".jfr");
      recording.dump(file.getAbsolutePath());
      return file;
    } catch (JfrStreamingException internalError) {
      throw new IOException(internalError);
    }
  }

  /** Action to be performed on a CPU breach. */
  public void performCpuProfile(AlertBreach alertBreach) {
    LOGGER.info("Received CPU alert, profiling");
    profileAndUpload(
        alertBreach, Duration.ofSeconds(alertBreach.getAlertConfiguration().getProfileDuration()));
  }

  /** Action to be performed on a MEMORY breach. */
  public void performMemoryProfile(AlertBreach alertBreach) {
    LOGGER.info("Received Memory alert, profiling");
    profileAndUpload(
        alertBreach, Duration.ofSeconds(alertBreach.getAlertConfiguration().getProfileDuration()));
  }

  /** Action to be performed on a MANUAL profile request. */
  public void performManualProfile(AlertBreach alertBreach) {
    LOGGER.info("Received manual alert, profiling");
    profileAndUpload(
        alertBreach, Duration.ofSeconds(alertBreach.getAlertConfiguration().getProfileDuration()));
  }

  /** Action to be performed on a periodic profile request. */
  public void performPeriodicProfile() {
    LOGGER.info("Received periodic profile request");
    profileAndUpload(
        new AlertBreach(AlertMetricType.PERIODIC, 0, periodicConfig),
        Duration.ofSeconds(periodicConfig.getProfileDuration()));
  }

  /** Dispatch alert breach event to handler. */
  @Override
  public void accept(AlertBreach alertBreach) {
    switch (alertBreach.getType()) {
      case CPU:
        performCpuProfile(alertBreach);
        break;

      case MEMORY:
        performMemoryProfile(alertBreach);
        break;

      case MANUAL:
        performManualProfile(alertBreach);
        break;

      case PERIODIC:
        performPeriodicProfile();
        break;
    }
  }
}
