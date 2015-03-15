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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ProcessCpuPerformanceCounterTest {
    private static final class TelemetryClassStub extends TelemetryClient {
        private final PerformanceCounter performanceCounter;

        public TelemetryClassStub(PerformanceCounter performanceCounter) {
            this.performanceCounter = performanceCounter;
        }

        public void track(Telemetry telemetry) {
            if (!(telemetry instanceof PerformanceCounterTelemetry)) {
                assertFalse(true);
            }

            PerformanceCounterTelemetry pct = (PerformanceCounterTelemetry)telemetry;
            assertTrue(pct.getCategoryName().startsWith("Process("));
            assertEquals(pct.getCounterName(), Constants.PROCESS_MEM_PC_COUNTER_NAME);
            assertEquals(pct.getInstanceName(), "");
        }
    }

    @Test
    public void testGetId() {
        ProcessCpuPerformanceCounter pc = new ProcessCpuPerformanceCounter();
        assertEquals(pc.getId(), Constants.PROCESS_CPU_PC_ID);
    }
}
