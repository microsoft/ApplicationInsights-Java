// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.microsoft.applicationinsights.diagnostics.collection.SystemStatsReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.OperatingSystemDetector;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import jdk.jfr.FlightRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides a JFR specific implementation of a diagnostics engine */
@SuppressWarnings("Java8ApiChecker")
public class CodeOptimizerDiagnosticsJfrInit {

  private static final Logger logger =
      LoggerFactory.getLogger(CodeOptimizerDiagnosticsJfrInit.class);
  private static final AtomicBoolean running = new AtomicBoolean(false);
  private static final AtomicInteger exceptionLogCount = new AtomicInteger(0);
  private static final AtomicInteger telemetryFailureLogCount = new AtomicInteger(0);

  private static final Runnable readCGroupData = CodeOptimizerDiagnosticsJfrInit::emitCGroupData;
  private static final AtomicReference<Runnable> telemetryEmitter = new AtomicReference<>(null);

  private CodeOptimizerDiagnosticsJfrInit() {}

  private static Runnable emitTelemetry(SystemStatsReader statsReader) {
    return () -> {
      try {
        if (statsReader != null && statsReader.isOpen()) {
          List<Double> telemetry = statsReader.readTelemetry();

          if (telemetry != null && telemetry.size() > 0) {
            new Telemetry().setTelemetry(telemetry).commit();
          } else {
            logFailure("No telemetry data present", null, telemetryFailureLogCount);
          }
        } else {
          logFailure("Stats reader not present", null, telemetryFailureLogCount);
        }
      } catch (RuntimeException | OperatingSystemInteractionException e) {
        logFailure("Reading Telemetry Failed", e, exceptionLogCount);
      } catch (SystemStatsReader.ReaderClosedException e) {
        // To be expected sometimes
      }
    };
  }

  private static void logFailure(String logLine, @Nullable Exception e, AtomicInteger count) {
    if ((count.get() % 100) == 0) {
      if (e != null) {
        logger.warn(logLine, e);
      } else {
        logger.warn(logLine);
      }

      count.set(0);
    }

    count.incrementAndGet();
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static void emitCGroupData() {
    try {
      CGroupData cgroupData = SystemStatsProvider.getCGroupData();

      if (cgroupData != null) {
        cgroupData.commit();
      }
    } catch (RuntimeException e) {
      logger.warn("Reading Cgroup Data Failed", e);
    }
  }

  public static boolean isOsSupported() {
    return OperatingSystemDetector.getOperatingSystem().supportsDiagnostics();
  }

  public static void initFeature(int thisPid) {
    if (!isOsSupported()) {
      return;
    }

    // eagerly get stats to warm it up
    SystemStatsProvider.init(thisPid);
  }

  public static void start(int thisPidSupplier) {
    if (!isOsSupported()) {
      return;
    }

    if (running.compareAndSet(false, true)) {
      SystemStatsReader statsReader = SystemStatsProvider.getStatsReader(thisPidSupplier);
      Runnable emitter = emitTelemetry(statsReader);
      if (telemetryEmitter.compareAndSet(null, emitter)) {
        FlightRecorder.addPeriodicEvent(Telemetry.class, emitter);
      } else {
        try {
          statsReader.close();
        } catch (IOException e) {
          logger.error("Failed to init stats reader", e);
        }
      }
      FlightRecorder.addPeriodicEvent(CGroupData.class, readCGroupData);

      readCGroupData.run();
    }
  }

  public static void stop() {
    if (!isOsSupported()) {
      return;
    }

    if (running.compareAndSet(true, false)) {
      if (telemetryEmitter.get() != null) {
        FlightRecorder.removePeriodicEvent(telemetryEmitter.get());
        telemetryEmitter.set(null);
      }
      FlightRecorder.removePeriodicEvent(readCGroupData);
      SystemStatsProvider.close();
    }
  }
}
