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

/** Created by gupele on 3/8/2015. */
public final class Constants {
  public static final String PERFORMANCE_COUNTER_PREFIX = "JSDK_";

  public static final String TOTAL_CPU_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "TotalCpuPerformanceCounter";

  public static final String PROCESS_CPU_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "ProcessCpuPerformanceCounter";

  public static final String TOTAL_CPU_PC_CATEGORY_NAME = "Processor";
  public static final String CPU_PC_COUNTER_NAME = "% Processor Time";

  public static final String TOTAL_MEMORY_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "TotalMemoryPerformanceCounter";
  public static final String PROCESS_MEM_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "ProcessMemoryPerformanceCounter";

  public static final String PROCESS_MEM_PC_COUNTER_NAME = "Private Bytes";

  public static final String TOTAL_MEMORY_PC_CATEGORY_NAME = "Memory";
  public static final String TOTAL_MEMORY_PC_COUNTER_NAME = "Available Bytes";

  public static final String PROCESS_IO_PC_ID =
      PERFORMANCE_COUNTER_PREFIX + "ProcessIOPerformanceCounter";
  public static final String PROCESS_IO_PC_COUNTER_NAME = "IO Data Bytes/sec";

  public static final String INSTANCE_NAME_TOTAL = "_Total";

  public static final String PROCESS_CATEGORY = "Process";

  private Constants() {}
}
