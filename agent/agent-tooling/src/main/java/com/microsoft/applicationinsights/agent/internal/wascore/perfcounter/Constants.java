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

package com.microsoft.applicationinsights.agent.internal.wascore.perfcounter;

public final class Constants {
  public static final String PERFORMANCE_COUNTER_PREFIX = "JSDK_";

  public static final String PROCESS_CPU_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "ProcessCpuPerformanceCounter";

  public static final String TOTAL_CPU_PC_METRIC_NAME = "\\Processor(_Total)\\% Processor Time";
  public static final String PROCESS_CPU_PC_METRIC_NAME =
      "\\Process(??APP_WIN32_PROC??)\\% Processor Time";

  public static final String TOTAL_MEMORY_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "TotalMemoryPerformanceCounter";
  public static final String PROCESS_MEM_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "ProcessMemoryPerformanceCounter";

  public static final String PROCESS_MEM_PC_METRICS_NAME =
      "\\Process(??APP_WIN32_PROC??)\\Private Bytes";

  public static final String TOTAL_MEMORY_PC_METRIC_NAME = "\\Memory\\Available Bytes";

  public static final String PROCESS_IO_PC_METRIC_NAME =
      "\\Process(??APP_WIN32_PROC??)\\IO Data Bytes/sec";

  private Constants() {}
}
