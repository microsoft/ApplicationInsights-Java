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

package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * The class supplies the overall memory usage of the machine.
 *
 * <p>Created by gupele on 3/9/2015.
 */
final class UnixTotalMemoryPerformanceCounter extends AbstractUnixPerformanceCounter {
  private static final String MEM_FILE = "/proc/meminfo";
  private static final double KB = 1024.0;

  public UnixTotalMemoryPerformanceCounter() {
    super(MEM_FILE);
  }

  @Override
  public String getId() {
    return Constants.TOTAL_MEMORY_PC_ID;
  }

  @Override
  public void report(TelemetryClient telemetryClient) {
    Double totalAvailableMemory = getTotalAvailableMemory();
    if (totalAvailableMemory == null) {
      return;
    }

    InternalLogger.INSTANCE.trace(
        "Sending Performance Counter: %s %s: %s",
        Constants.TOTAL_MEMORY_PC_CATEGORY_NAME,
        Constants.TOTAL_MEMORY_PC_COUNTER_NAME,
        totalAvailableMemory);
    Telemetry telemetry =
        new PerformanceCounterTelemetry(
            Constants.TOTAL_MEMORY_PC_CATEGORY_NAME,
            Constants.TOTAL_MEMORY_PC_COUNTER_NAME,
            "",
            totalAvailableMemory);

    telemetryClient.track(telemetry);
  }

  private Double getTotalAvailableMemory() {
    BufferedReader bufferedReader = null;

    Double result = null;
    UnixTotalMemInfoParser reader = new UnixTotalMemInfoParser();
    try {
      bufferedReader = new BufferedReader(new FileReader(getProcessFile()));
      String line;
      while (!reader.done() && (line = bufferedReader.readLine()) != null) {
        reader.process(line);
      }

      // The value we get is in KB so we need to translate that to bytes.
      result = reader.getValue() * KB;
    } catch (Exception e) {
      result = null;
      logPerfCounterErrorError("Error while parsing file: '%s'", e.toString());
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (Exception e) {
          logPerfCounterErrorError("Error while closing file : '%s'", e.toString());
        }
      }
    }

    return result;
  }
}
