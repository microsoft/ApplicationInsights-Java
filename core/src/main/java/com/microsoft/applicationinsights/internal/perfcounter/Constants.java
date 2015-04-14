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

/**
 * Created by gupele on 3/8/2015.
 */
public final class Constants {
    public final static String PERFORMANCE_COUNTER_PREFIX = "JSDK_";

    public final static String TOTAL_CPU_PC_ID = PERFORMANCE_COUNTER_PREFIX + "TotalCpuPerformanceCounter";

    public final static String PROCESS_CPU_PC_ID = PERFORMANCE_COUNTER_PREFIX + "ProcessCpuPerformanceCounter";

    public final static String TOTAL_CPU_PC_CATEGORY_NAME = "Processor";
    public final static String CPU_PC_COUNTER_NAME = "% Processor Time";


    public final static String TOTAL_MEMORY_PC_ID = PERFORMANCE_COUNTER_PREFIX + "TotalMemoryPerformanceCounter";
    public final static String PROCESS_MEM_PC_ID = PERFORMANCE_COUNTER_PREFIX + "ProcessMemoryPerformanceCounter";

    public final static String PROCESS_MEM_PC_COUNTER_NAME = "Private Bytes";

    public final static String TOTAL_MEMORY_PC_CATEGORY_NAME = "Memory";
    public final static String TOTAL_MEMORY_PC_COUNTER_NAME = "Available Bytes";


    public final static String PROCESS_IO_PC_ID = PERFORMANCE_COUNTER_PREFIX + "ProcessIOPerformanceCounter";
    public final static String PROCESS_IO_PC_COUNTER_NAME = "IO Data Bytes/sec";

    public final static double DEFAULT_DOUBLE_VALUE = -1.0;

    private Constants() {
    }
}
