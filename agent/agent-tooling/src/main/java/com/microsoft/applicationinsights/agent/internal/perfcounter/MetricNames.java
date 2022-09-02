// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

public final class MetricNames {

  // this is reported (correctly) as "normalized" (divided by # of CPU cores)
  public static final String TOTAL_CPU_PERCENTAGE = "\\Processor(_Total)\\% Processor Time";

  // unfortunately the Java SDK behavior has always been to report the "% Processor Time" number
  // as "normalized" (divided by # of CPU cores), even though it should be non-normalized
  // we cannot change this existing behavior as it would break existing customers' alerts, but at
  // least there is a configuration option that gives users a way to opt in to the correct behavior
  //
  // note: the normalized value is now separately reported under a different metric
  // "% Processor Time Normalized"
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
