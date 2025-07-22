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
import com.microsoft.applicationinsights.diagnostics.jfr.MachineStats;
import com.microsoft.applicationinsights.diagnostics.jfr.SystemStatsProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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
  private int thisPid;

  public CodeOptimizerDiagnosticEngineJfr(ScheduledExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void init(int thisPid) {
    if (!CodeOptimizerDiagnosticsJfrInit.isOsSupported()) {
      logger.warn("Code Optimizer diagnostics is not supported on this operating system");
      return;
    }

    this.thisPid = thisPid;

    logger.debug("Initialising Code Optimizer Diagnostic Engine");
    CodeOptimizerDiagnosticsJfrInit.initFeature(thisPid);
    logger.debug("Code Optimizer Diagnostic Engine Initialised");
  }

  private static void startDiagnosticCycle(int thisPid) {
    logger.debug("Starting Code Optimizer Diagnostic Cycle");
    CodeOptimizerDiagnosticsJfrInit.initFeature(thisPid);
    CodeOptimizerDiagnosticsJfrInit.start(thisPid);
  }

  private static void endDiagnosticCycle() {
    logger.debug("Ending Code Optimizer Diagnostic Cycle");
    CodeOptimizerDiagnosticsJfrInit.stop();
  }

  @Override
  public Future<DiagnosisResult<?>> performDiagnosis(AlertBreach alert) {
    CompletableFuture<DiagnosisResult<?>> diagnosisResultCompletableFuture =
        new CompletableFuture<>();
    try {
      if (semaphore.tryAcquire(SEMAPHORE_TIMEOUT_IN_SEC, TimeUnit.SECONDS)) {
        emitInfo(alert);

        long profileDurationInSec = alert.getAlertConfiguration().getProfileDurationSeconds();

        long end = profileDurationInSec - TIME_BEFORE_END_OF_PROFILE_TO_EMIT_EVENT;

        startDiagnosticCycle(thisPid);

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

            logger.debug("Shutting down diagnostic cycle");
            endDiagnosticCycle();
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

  private static void emitInfo(AlertBreach alert) {
    logger.debug("Emitting Code Optimizer Diagnostic Event");
    emitAlertBreachJfrEvent(alert);
    CodeOptimizerDiagnosticsJfrInit.emitCGroupData();
    emitMachineStats();
  }

  private static void emitMachineStats() {
    MachineStats machineStats = SystemStatsProvider.getMachineStats();
    machineStats.commit();
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
