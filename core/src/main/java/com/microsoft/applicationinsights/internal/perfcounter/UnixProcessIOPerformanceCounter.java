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

import java.io.BufferedReader;
import java.io.FileReader;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The class knows how to supply the io usage of the current process under the Unix OS.
 *
 * When activated the class will calculate the io usage based on the data under /proc/[pid]/io
 * file and will create a {@link com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry}
 * that will contain that data per the amount of time elapsed from the last check in seconds.
 *
 * Created by gupele on 3/8/2015.
 */
final class UnixProcessIOPerformanceCounter extends AbstractUnixPerformanceCounter {
    private final static double NANOS_IN_SECOND = 1000000000.0;

    private double prevProcessIO;

    private long lastCollectionInNanos = -1;

    public UnixProcessIOPerformanceCounter() {
        super("/proc/" + SystemInformation.INSTANCE.getProcessId() + "/io");
    }

    @Override
    public String getId() {
        return Constants.PROCESS_IO_PC_ID;
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        long currentCollectionInNanos = System.nanoTime();

        Double processIO = getCurrentIOForCurrentProcess();
        if (processIO == null) {
            return;
        }
        if (lastCollectionInNanos != -1) {
            // Not the first time

            double timeElapsedInSeconds = ((double)(currentCollectionInNanos - lastCollectionInNanos)) / NANOS_IN_SECOND;

            double value = (processIO - prevProcessIO) / timeElapsedInSeconds;
            prevProcessIO = processIO;

            InternalLogger.INSTANCE.trace("Sending Performance Counter: %s %s: %s", getProcessCategoryName(), Constants.PROCESS_IO_PC_COUNTER_NAME, value);
            Telemetry telemetry = new PerformanceCounterTelemetry(
                    getProcessCategoryName(),
                    Constants.PROCESS_IO_PC_COUNTER_NAME,
                    SystemInformation.INSTANCE.getProcessId(),
                    value);

            telemetryClient.track(telemetry);
        }

        prevProcessIO = processIO;
        lastCollectionInNanos = currentCollectionInNanos;
    }

    /**
     *
     * @return the current IO for current process, or null if the datum could not be measured.
     */
    public Double getCurrentIOForCurrentProcess() {
        BufferedReader bufferedReader = null;

        Double result = null;
        UnixProcessIOtParser parser = new UnixProcessIOtParser();
        try {
            bufferedReader = new BufferedReader(new FileReader(getProcessFile()));
            String line;
            while (!parser.done() && (line = bufferedReader.readLine()) != null) {
                parser.process(line);
            }

            result = parser.getValue();
        } catch (Exception e) {
            result = null;
            logPerfCounterErrorError("Error while parsing file: '%s'", getId());
            InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
        } finally {
            if (bufferedReader != null ) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    logPerfCounterErrorError("Error while closing file : '%s'", e.toString());
                    InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
                }
            }
        }

        return result;
    }
}
