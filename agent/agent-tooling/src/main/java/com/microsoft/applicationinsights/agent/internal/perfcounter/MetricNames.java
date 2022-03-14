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

package com.microsoft.applicationinsights.agent.internal.perfcounter;

public final class MetricNames {

  // TODO (trask) should this be normalized or not? (currently we are reporting it normalized)
  public static final String TOTAL_CPU_PERCENTAGE = "\\Processor(_Total)\\% Processor Time";

  // unfortunately the Java SDK behavior has always been to report the "% Processor Time" number
  // as "normalized" (divided by # of CPU cores), even though it should be non-normalized
  // maybe this can be fixed in 4.0 (would be a breaking change)
  public static final String PROCESS_CPU_PERCENTAGE =
      "\\Process(??APP_WIN32_PROC??)\\% Processor Time";

  // introduced in 3.3.0
  public static final String PROCESS_CPU_PERCENTAGE_NORMALIZED =
      "\\Process(??APP_WIN32_PROC??)\\% Processor Time Normalized";

  public static final String PROCESS_MEMORY = "\\Process(??APP_WIN32_PROC??)\\Private Bytes";

  public static final String TOTAL_MEMORY = "\\Memory\\Available Bytes";

  public static final String PROCESS_IO = "\\Process(??APP_WIN32_PROC??)\\IO Data Bytes/sec";

  private MetricNames() {}
}
