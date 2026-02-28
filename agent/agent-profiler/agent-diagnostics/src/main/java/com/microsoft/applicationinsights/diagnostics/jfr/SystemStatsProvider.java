// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.microsoft.applicationinsights.diagnostics.collection.SystemStatsReader;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.Calibration;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.Calibrator;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.CalibratorDefault;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.ContextSwitchingRunner;
import com.microsoft.applicationinsights.diagnostics.collection.cores.RuntimeCoreCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfoReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupUsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelMonitorDeviceDriver;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.OperatingSystemDetector;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.LinuxKernelMonitor;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.LinuxMemoryInfoReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.LinuxProcessDumper;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups.LinuxCGroupDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups.LinuxCGroupUsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2.LinuxCGroupV2DataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2.LinuxCGroupV2UsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpCGroupDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpCGroupUsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpKernelMonitor;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpMemoryInfoReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpProcessDumper;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.Process;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessDumper;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ThisPidSupplier;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes components mostly as singletons, mostly required to aid the migration away from being
 * part of a DI framework which managed lifecycles.
 */
@SuppressWarnings("Java8ApiChecker") // JFR APIs require Java 11+, but agent targets Java 8 bytecode
public class SystemStatsProvider {
  private static final Logger logger = LoggerFactory.getLogger(SystemStatsProvider.class);

  private static final AtomicBoolean initialised = new AtomicBoolean(false);

  private static final Map<Class<?>, AtomicReference<?>> singletons = new HashMap<>();

  private SystemStatsProvider() {}

  public static void init(int thisPid, Path cgroupBasePath) {
    // Ensure we only initialize once
    if (initialised.compareAndSet(false, true)) {
      singletons.put(ThisPidSupplier.class, new AtomicReference<>((ThisPidSupplier) () -> thisPid));

      if (singletons.get(Calibrator.class) == null) {
        try {
          getCalibration();
          getMachineStats();
          getCGroupData(cgroupBasePath);

          // Close until needed
          close();
        } catch (RuntimeException e) {
          logger.error("Failed to initialise Code Optimizer", e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked") // safe unchecked cast - type verified by runtime context
  private static <T> T getSingleton(Class<T> clazz) {
    return (T) singletons.get(clazz).get();
  }

  @SuppressWarnings("unchecked") // safe unchecked cast - type verified by runtime context
  private static <T> T getSingleton(Class<T> clazz, Supplier<T> supplier) {
    AtomicReference<T> current = (AtomicReference<T>) singletons.get(clazz);

    if (current == null) {
      current = new AtomicReference<>();
      singletons.put(clazz, current);
    }

    if (current.get() == null) {
      T instance = supplier.get();
      if (!current.compareAndSet(null, instance)) {
        // If we failed to create a Closeable resource, close it
        if (instance instanceof Closeable) {
          try {
            ((Closeable) instance).close();
          } catch (IOException e) {
            logger.error("Failed to close", e);
          }
        }
      }
    }

    return current.get();
  }

  @SuppressWarnings({
    "checkstyle:AbbreviationAsWordInName",
    "MemberName"
  }) // CGroup is the standard abbreviation for Control Group
  public static CGroupData getCGroupData(Path cgroupBasePath) {
    return getSingleton(
        CGroupData.class,
        () -> {
          try {
            CGroupDataReader reader = buildCGroupDataReader(cgroupBasePath);
            CGroupData data = new CGroupData();

            return data.setKmemLimit(reader.getKmemLimit())
                .setMemoryLimit(reader.getMemoryLimit())
                .setMemorySoftLimit(reader.getMemorySoftLimit())
                .setCpuLimit(reader.getCpuLimit())
                .setCpuPeriod(reader.getCpuPeriod());

          } catch (RuntimeException | OperatingSystemInteractionException e) {
            logger.warn("No CGroup data present");
            return null;
          }
        });
  }

  public static MachineStats getMachineStats() {
    return getSingleton(
        MachineStats.class,
        () ->
            new MachineStats()
                .setContextSwitchesPerMs(getCalibration().getContextSwitchingRate())
                .setCoreCount(new RuntimeCoreCounter().getCoreCount()));
  }

  private static Calibration getCalibration() {
    return getSingleton(
        Calibration.class,
        () -> {
          Calibrator calibrator =
              new CalibratorDefault(
                  new ContextSwitchingRunner(), getKernelMonitor(), getThisProcess());

          return calibrator.calibrate();
        });
  }

  private static Process getThisProcess() {
    return getSingleton(
        Process.class,
        () -> {
          ProcessDumper processDumper = getProcessDumper();
          if (processDumper == null) {
            return null;
          }
          processDumper.poll();

          Process thisProcess = processDumper.thisProcess();
          processDumper.closeProcesses(Collections.singletonList(thisProcess.getPid()));
          return thisProcess;
        });
  }

  @SuppressWarnings(
      "checkstyle:AbbreviationAsWordInName") // CGroup is the standard abbreviation for Control
  // Group
  private static CGroupDataReader buildCGroupDataReader(Path cgroupBasePath) {
    switch (OperatingSystemDetector.getOperatingSystem()) {
      case LINUX:
        CGroupDataReader dataReader = new LinuxCGroupDataReader(cgroupBasePath);
        if (dataReader.isAvailable()) {
          return dataReader;
        }

        dataReader = new LinuxCGroupV2DataReader(cgroupBasePath);
        if (dataReader.isAvailable()) {
          return dataReader;
        }

        logger.info("No CGroup limits data not found");
        return new NoOpCGroupDataReader();
      default:
        return new NoOpCGroupDataReader();
    }
  }

  @SuppressWarnings(
      "checkstyle:AbbreviationAsWordInName") // CGroup is the standard abbreviation for Control
  // Group
  private static ProcessDumper getProcessDumper() {
    ThisPidSupplier pidSupplier = getSingleton(ThisPidSupplier.class);
    switch (OperatingSystemDetector.getOperatingSystem()) {
      case LINUX:
        return new LinuxProcessDumper(false, pidSupplier.get());
      default:
        return new NoOpProcessDumper();
    }
  }

  @SuppressWarnings(
      "checkstyle:AbbreviationAsWordInName") // CGroup is the standard abbreviation for Control
  // Group
  private static CGroupUsageDataReader buildCGroupUsageDataReader(Path cgroupBasePath) {
    return getSingleton(
        CGroupUsageDataReader.class,
        () -> {
          if (cgroupBasePath == null) {
            logger.info("No CGroup data present");
            return new NoOpCGroupUsageDataReader();
          }

          switch (OperatingSystemDetector.getOperatingSystem()) {
            case LINUX:
              CGroupUsageDataReader usageReader = new LinuxCGroupUsageDataReader(cgroupBasePath);
              if (usageReader.isAvailable()) {
                return usageReader;
              }

              usageReader = new LinuxCGroupV2UsageDataReader(cgroupBasePath);
              if (usageReader.isAvailable()) {
                return usageReader;
              }

              logger.warn("CGroup data not found");
              return new NoOpCGroupUsageDataReader();
            default:
              return new NoOpCGroupUsageDataReader();
          }
        });
  }

  private static MemoryInfoReader buildMemoryInfoReader() {
    return getSingleton(
        MemoryInfoReader.class,
        () -> {
          switch (OperatingSystemDetector.getOperatingSystem()) {
            case LINUX:
              return new LinuxMemoryInfoReader();
            default:
              return new NoOpMemoryInfoReader();
          }
        });
  }

  private static SystemStatsReader getSystemStatsReader(Path cgroupBasePath) {
    return getSingleton(SystemStatsReader.class, () -> buildSystemStatsReader(cgroupBasePath));
  }

  private static SystemStatsReader buildSystemStatsReader(Path cgroupBasePath) {
    SystemStatsReader ssr =
        new SystemStatsReader(
            getKernelMonitor(),
            buildCGroupUsageDataReader(cgroupBasePath),
            getThisProcess().getCpuStats(),
            getThisProcess().getIoStats(),
            buildMemoryInfoReader());
    try {
      ssr.readTelemetry();
      ssr.readTelemetry();
    } catch (OperatingSystemInteractionException | SystemStatsReader.ReaderClosedException e) {
      logger.warn("Failed to read telemetry", e);
    }
    return ssr;
  }

  private static KernelMonitorDeviceDriver getKernelMonitor() {
    return getSingleton(
        KernelMonitorDeviceDriver.class,
        () -> {
          switch (OperatingSystemDetector.getOperatingSystem()) {
            case LINUX:
              return new LinuxKernelMonitor();
            case WINDOWS:
            case SOLARIS:
            case MAC_OS:
            default:
              return new NoOpKernelMonitor();
          }
        });
  }

  public static SystemStatsReader getStatsReader(int thisPidSupplier, Path cgroupBasePath) {
    init(thisPidSupplier, cgroupBasePath);
    return getSystemStatsReader(cgroupBasePath);
  }

  public static void close() {
    // Shutdown all closable singletons
    singletons.forEach(
        (clazz, supplier) -> {
          Object instance = supplier.get();
          if (instance instanceof Closeable) {
            try {
              ((Closeable) instance).close();
            } catch (IOException e) {
              logger.error("Failed to close reader", e);
            }

            singletons.get(clazz).set(null);
          }
        });
  }
}
