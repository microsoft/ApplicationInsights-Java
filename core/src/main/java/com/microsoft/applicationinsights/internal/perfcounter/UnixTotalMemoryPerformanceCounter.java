/*
 * AppInsights-Java
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
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * The class supplies the overall memory usage of the machine.
 *
 * Created by gupele on 3/9/2015.
 */
final class UnixTotalMemoryPerformanceCounter extends AbstractUnixPerformanceCounterBase {
    private final static String MEM_FILE = "/proc/meminfo";
    private final static String MEM_FREE_PREFIX = "MemFree:";
    private final static String BUFFERS_PREFIX = "Buffers";
    private final static String CACHED_PREFIX = "Cached";

    // An helper class for the parsing stage.
    private static final class ParsingData {
        public int doneCounter;
        public double returnValue = Constants.DEFAULT_DOUBLE_VALUE;

        public ParsingData(int doneCounter) {
            this.doneCounter = doneCounter;
        }
    }

    public UnixTotalMemoryPerformanceCounter() {
        super(MEM_FILE);
    }

    @Override
    public String getId() {
        return Constants.TOTAL_MEMORY_PC_ID;
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        double totalMemoryUsage = getTotalMemoryUsage();
        Telemetry telemetry = new PerformanceCounterTelemetry(
                Constants.TOTAL_MEMORY_PC_CATEGORY_NAME,
                Constants.TOTAL_MEMORY_PC_COUNTER_NAME,
                "",
                totalMemoryUsage);

        telemetryClient.track(telemetry);
    }

    private double getTotalMemoryUsage() {
        BufferedReader bufferedReader = null;

        boolean memFreeDone = false;
        boolean buffersDone = false;
        boolean cachedDone = false;
        ParsingData parsingData = new ParsingData(3);
        double result = Constants.DEFAULT_DOUBLE_VALUE;
        try {
            bufferedReader = new BufferedReader(new FileReader(getProcessFile()));
            String line;
            while (parsingData.doneCounter != 0 && (line = bufferedReader.readLine()) != null) {
                if (!memFreeDone) {
                    if (parseValue(parsingData, line, MEM_FREE_PREFIX)) {
                        memFreeDone = true;
                        continue;
                    }
                }
                if (!buffersDone) {
                    if (parseValue(parsingData, line, BUFFERS_PREFIX)) {
                        buffersDone = true;
                        continue;
                    }
                }
                if (!cachedDone) {
                    if (parseValue(parsingData, line, CACHED_PREFIX)) {
                        cachedDone = true;
                        continue;
                    }
                }
            }

            result = parsingData.returnValue;
        } catch (Exception e) {
            result = Constants.DEFAULT_DOUBLE_VALUE;
            logError("Error while parsing file: '%s'", e.getMessage());
        } finally {
            if (bufferedReader != null ) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    logError("Error while closing file : '%s'", e.getMessage());
                }
            }
        }

        return result;
    }

    private boolean parseValue(ParsingData parsingData, String line, String part) {
        int index = line.indexOf(part);
        if (index != -1) {
            line.trim();
            String[] strings = line.split(" ");
            parsingData.returnValue += Double.valueOf(strings[strings.length - 2]);
            --(parsingData.doneCounter);
            return true;
        }

        return false;
    }
}
