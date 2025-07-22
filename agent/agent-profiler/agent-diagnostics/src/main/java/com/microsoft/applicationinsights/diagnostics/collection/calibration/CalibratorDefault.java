// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.calibration;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelMonitorDeviceDriver;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.Process;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalibratorDefault implements Calibrator {

  private static final Logger logger = LoggerFactory.getLogger(ContextSwitchingRunner.class);

  private final ContextSwitchingRunner contextSwitching;
  private final KernelMonitorDeviceDriver kernel;
  private final Process thisProcess;

  public CalibratorDefault(
      ContextSwitchingRunner contextSwitching,
      KernelMonitorDeviceDriver kernel,
      Process thisProcess) {
    this.contextSwitching = contextSwitching;
    this.kernel = kernel;
    this.thisProcess = thisProcess;
  }

  @Override
  public Calibration calibrate() {
    try {
      List<Double> contextSwitchesPerRun = new ArrayList<>();
      Iterator<Void> iterator = contextSwitching.iterator();

      int runCount = contextSwitching.getRunCount();
      double[] diagnosticsTimes = new double[runCount];
      double[] cpuTimes = new double[runCount];

      for (int i = 0; iterator.hasNext(); i++) {
        long time = System.currentTimeMillis();

        update(thisProcess);

        iterator.next();

        update(thisProcess);

        time = System.currentTimeMillis() - time;

        // MIN_VALUE to Make sure its not 0
        diagnosticsTimes[i] = safeDiv(getProcessCpuTime(thisProcess), (double) time);
        cpuTimes[i] = safeDiv(getCpuTime(), (double) time);

        double contextSwitches = getContextSwitches();

        contextSwitchesPerRun.add(safeDiv(contextSwitches, (double) time));
      }

      double maxContextSwitches = Collections.max(contextSwitchesPerRun);
      int index = contextSwitchesPerRun.indexOf(maxContextSwitches);

      double diagnosticsTime = diagnosticsTimes[index];
      double cpuTime = diagnosticsTimes[index];
      double timeDiagnosticsHadAvailable = safeDiv(diagnosticsTime, cpuTime);
      double contextSwitchingRate = safeDiv(maxContextSwitches, timeDiagnosticsHadAvailable);
      return new Calibration(contextSwitchingRate);
    } catch (Throwable e) {
      logger.debug("Completing exceptionally", e);
    }
    return new Calibration(Calibration.UNKNOWN);
  }

  private static double safeDiv(double numerator, double denominator) {
    return numerator / (denominator + Double.MIN_VALUE);
  }

  private void update(Process process) throws OperatingSystemInteractionException {
    process.poll();
    kernel.poll();

    process.update();
    kernel.update();
  }

  private static double getProcessCpuTime(Process process) {
    return process.getCpuStats().getTotalTime().doubleValue();
  }

  private long getContextSwitches() throws OperatingSystemInteractionException {
    return kernel.getCounters().getContextSwitches();
  }

  private double getCpuTime() throws OperatingSystemInteractionException {
    return 100D - kernel.getCounters().getIdleTime();
  }
}
