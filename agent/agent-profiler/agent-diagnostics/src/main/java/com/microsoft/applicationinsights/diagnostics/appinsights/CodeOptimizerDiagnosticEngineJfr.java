// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.appinsights;

import com.azure.json.JsonProviders;
import com.azure.json.JsonWriter;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.diagnostics.DiagnosisResult;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngine;
import com.microsoft.applicationinsights.diagnostics.jfr.AlertBreachJfrEvent;
import com.microsoft.applicationinsights.diagnostics.jfr.CodeOptimizerDiagnosticsJfrInit;
import com.microsoft.applicationinsights.diagnostics.jfr.MachineInfo;
import com.microsoft.applicationinsights.diagnostics.jfr.SystemStatsProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for Code Optimizer diagnostics. Provides the functionality to initialize engine and
 * coordinate emitting diagnostics on a breach.
 */
public class CodeOptimizerDiagnosticEngineJfr implements DiagnosticEngine {
  private static final Logger logger =
      LoggerFactory.getLogger(CodeOptimizerDiagnosticEngineJfr.class);
  public static final int SEMAPHORE_TIMEOUT_IN_SEC = 10;
  public static final long TIME_BEFORE_END_OF_PROFILE_TO_EMIT_EVENT = 10L;
  private final ScheduledExecutorService executorService;
  private final Semaphore semaphore = new Semaphore(1, false);
  private final Path cgroupBasePath;
  private final AtomicInteger thisPid = new AtomicInteger();

  // When true, periodic diagnostic emitters are registered continuously (for continuous profiling)
  // and must not be torn down by an individual performDiagnosis cycle.
  private final AtomicBoolean continuous = new AtomicBoolean(false);

  // Guards transitions of the continuous flag against the teardown performed at the end of a
  // (non-continuous) diagnostic cycle, so that a breach processed during startup cannot tear down
  // the continuously-registered emitters once continuous profiling has been enabled.
  private final Object continuousLifecycleLock = new Object();

  public CodeOptimizerDiagnosticEngineJfr(
      ScheduledExecutorService executorService, Path cgroupBasePath) {
    this.executorService = executorService;
    this.cgroupBasePath = cgroupBasePath;
  }

  @Override
  public void init(int thisPid) {
    if (!isOsSupported()) {
      logger.warn("Code Optimizer diagnostics is not supported on this operating system");
      return;
    }

    this.thisPid.set(thisPid);

    logger.debug("Initialising Code Optimizer Diagnostic Engine");
    CodeOptimizerDiagnosticsJfrInit.initFeature(thisPid, cgroupBasePath);
    logger.debug("Code Optimizer Diagnostic Engine Initialised");
  }

  // visible for testing
  protected boolean isOsSupported() {
    return CodeOptimizerDiagnosticsJfrInit.isOsSupported();
  }

  // visible for testing
  protected void startDiagnosticCycle() {
    logger.debug("Starting Code Optimizer Diagnostic Cycle");
    int pid = thisPid.get();
    CodeOptimizerDiagnosticsJfrInit.initFeature(pid, cgroupBasePath);
    CodeOptimizerDiagnosticsJfrInit.start(pid, cgroupBasePath);
  }

  // visible for testing
  protected void endDiagnosticCycle() {
    logger.debug("Ending Code Optimizer Diagnostic Cycle");
    CodeOptimizerDiagnosticsJfrInit.stop();
  }

  @Override
  public void startContinuousDiagnostics() {
    if (!isOsSupported()) {
      logger.warn("Code Optimizer diagnostics is not supported on this operating system");
      return;
    }

    synchronized (continuousLifecycleLock) {
      continuous.set(true);
      logger.debug("Starting continuous Code Optimizer diagnostics");
      // Registers the periodic diagnostic emitters (Telemetry, CGroupData) so they continuously
      // populate the continuous profiling circular buffer.
      startDiagnosticCycle();
    }
  }

  @Override
  public void stopContinuousDiagnostics() {
    if (!isOsSupported()) {
      return;
    }

    synchronized (continuousLifecycleLock) {
      continuous.set(false);
      logger.debug("Stopping continuous Code Optimizer diagnostics");
      endDiagnosticCycle();
    }
  }

  @Override
  public Future<DiagnosisResult<?>> performDiagnosis(AlertBreach alert) {
    if (continuous.get()) {
      // Periodic diagnostics are already running continuously, so we must not start or stop the
      // diagnostic cycle here (doing so would remove the continuously-registered emitters). Just
      // emit the point-in-time breach information.
      CompletableFuture<DiagnosisResult<?>> diagnosisResultCompletableFuture =
          new CompletableFuture<>();
      try {
        emitInfo(alert);
        diagnosisResultCompletableFuture.complete(null);
      } catch (RuntimeException e) {
        // The caller discards the returned future, so log here to avoid silently swallowing the
        // failure to emit breach diagnostics.
        logger.error("Failed to emit continuous diagnostic breach information", e);
        diagnosisResultCompletableFuture.completeExceptionally(e);
      }
      return diagnosisResultCompletableFuture;
    }

    CompletableFuture<DiagnosisResult<?>> diagnosisResultCompletableFuture =
        new CompletableFuture<>();
    try {
      if (semaphore.tryAcquire(SEMAPHORE_TIMEOUT_IN_SEC, TimeUnit.SECONDS)) {
        emitInfo(alert);

        long profileDurationInSec = alert.getAlertConfiguration().getProfileDurationSeconds();

        long end = profileDurationInSec - TIME_BEFORE_END_OF_PROFILE_TO_EMIT_EVENT;

        startDiagnosticCycle();

        scheduleEmittingAlertBreachEvent(alert, end);

        scheduleShutdown(alert, diagnosisResultCompletableFuture, end);

        return diagnosisResultCompletableFuture;
      }
    } catch (InterruptedException e) {
      semaphore.release();
      diagnosisResultCompletableFuture.completeExceptionally(e);
      return diagnosisResultCompletableFuture;
    }
    diagnosisResultCompletableFuture.completeExceptionally(
        new RuntimeException("Failed to obtain diagnosis lock"));
    return diagnosisResultCompletableFuture;
  }

  private void scheduleShutdown(
      AlertBreach alert,
      CompletableFuture<DiagnosisResult<?>> diagnosisResultCompletableFuture,
      long end) {
    executorService.schedule(
        () -> {
          try {
            emitInfo(alert);

            // We do not return a result atm
            diagnosisResultCompletableFuture.complete(null);

            // Only tear down the diagnostic cycle if continuous diagnostics has not been enabled in
            // the meantime. If a breach is processed during startup, before
            // startContinuousDiagnostics
            // has run, this shutdown would otherwise permanently stop the continuously-registered
            // emitters once continuous profiling starts. The lock ensures the check-and-stop cannot
            // interleave with a concurrent startContinuousDiagnostics.
            synchronized (continuousLifecycleLock) {
              if (continuous.get()) {
                logger.debug("Continuous diagnostics is active; leaving diagnostic cycle running");
              } else {
                logger.debug("Shutting down diagnostic cycle");
                endDiagnosticCycle();
              }
            }
          } catch (RuntimeException e) {
            logger.error("Failed to shutdown cleanly", e);
          } finally {
            semaphore.release();
          }
        },
        end,
        TimeUnit.SECONDS);
  }

  private void scheduleEmittingAlertBreachEvent(AlertBreach alert, long end) {
    // Schedule emitting JFR data halfway through to try to ensure it makes it in to the profile
    executorService.schedule(
        () -> {
          try {
            emitInfo(alert);
          } catch (RuntimeException e) {
            logger.error("Failed to emit breach", e);
          }
        },
        end / 2,
        TimeUnit.SECONDS);
  }

  // visible for testing
  protected void emitInfo(AlertBreach alert) {
    logger.debug("Emitting Code Optimizer Diagnostic Event");
    emitAlertBreachJfrEvent(alert);
    CodeOptimizerDiagnosticsJfrInit.emitCGroupData(cgroupBasePath);
    emitMachineInfo();
  }

  private static void emitMachineInfo() {
    MachineInfo machineInfo = SystemStatsProvider.getMachineInfo();
    machineInfo.commit();
  }

  private static void emitAlertBreachJfrEvent(AlertBreach alert) {
    try (StringWriter stringWriter = new StringWriter();
        JsonWriter writer = JsonProviders.createWriter(stringWriter)) {
      alert.toJson(writer).flush();
      AlertBreachJfrEvent event = new AlertBreachJfrEvent().setAlertBreach(stringWriter.toString());
      event.commit();
      logger.debug("Emitted Code Optimizer Diagnostic Event");
    } catch (IOException e) {
      logger.error("Failed to create breach JFR event", e);
    }
  }
}
