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

package com.microsoft.applicationinsights.profileUploader;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Metadata used by service profiler */
public class ServiceProfilerIndex {
  public static final String SERVICE_PROFILER_SOURCE_PROPERTY_NAME = "Source";
  public static final String SERVICE_PROFILER_FILEID_PROPERTY_NAME = "FileId";
  public static final String SERVICE_PROFILER_STAMPID_PROPERTY_NAME = "StampId";
  public static final String SERVICE_PROFILER_DATACUBE_PROPERTY_NAME = "DataCube";
  public static final String SERVICE_PROFILER_MACHINENAME_PROPERTY_NAME = "MachineName";
  public static final String SERVICE_PROFILER_PROCESSID_PROPERTY_NAME = "ProcessId";
  public static final String SERVICE_PROFILER_ETLFILESESSIONID_PROPERTY_NAME = "EtlFileSessionId";
  public static final String SERVICE_PROFILER_OPERATINGSYSTEM_PROPERTY_NAME = "OperatingSystem";
  public static final String SERVICE_PROFILER_AVERAGECPUUSAGE_METRIC_NAME = "AverageCPUUsage";
  public static final String SERVICE_PROFILER_AVERAGE_MEMORY_USAGE_METRIC_NAME =
      "AverageMemoryUsage";
  public static final String SERVICE_PROFILER_ARTIFACT_KIND_NAME = "ArtifactKind";
  public static final String SERVICE_PROFILER_ARTIFACT_ID_NAME = "ArtifactId";
  public static final String SERVICE_PROFILER_EXTENSION_NAME = "Extension";

  private final Map<String, String> sampleEvent = new HashMap<>();
  private final Map<String, Double> metrics = new HashMap<>();

  public ServiceProfilerIndex(
      String triggeredBy,
      String fileId,
      String stampId,
      UUID dataCubeId,
      String timeStamp,
      String machineName,
      String os,
      String processId,
      String artifactKind,
      String artifactId,
      String extension,
      double cpuUsage,
      double memoryUsage) {
    sampleEvent.put(SERVICE_PROFILER_FILEID_PROPERTY_NAME, fileId);
    sampleEvent.put(SERVICE_PROFILER_STAMPID_PROPERTY_NAME, stampId);
    sampleEvent.put(SERVICE_PROFILER_DATACUBE_PROPERTY_NAME, dataCubeId.toString());
    sampleEvent.put(SERVICE_PROFILER_ETLFILESESSIONID_PROPERTY_NAME, timeStamp);
    sampleEvent.put(SERVICE_PROFILER_MACHINENAME_PROPERTY_NAME, machineName);
    sampleEvent.put(SERVICE_PROFILER_PROCESSID_PROPERTY_NAME, processId);
    sampleEvent.put(SERVICE_PROFILER_SOURCE_PROPERTY_NAME, triggeredBy);
    sampleEvent.put(SERVICE_PROFILER_OPERATINGSYSTEM_PROPERTY_NAME, os);
    sampleEvent.put(SERVICE_PROFILER_ARTIFACT_KIND_NAME, artifactKind);
    sampleEvent.put(SERVICE_PROFILER_ARTIFACT_ID_NAME, artifactId);
    sampleEvent.put(SERVICE_PROFILER_EXTENSION_NAME, extension);
    metrics.put(SERVICE_PROFILER_AVERAGECPUUSAGE_METRIC_NAME, cpuUsage);
    metrics.put(SERVICE_PROFILER_AVERAGE_MEMORY_USAGE_METRIC_NAME, memoryUsage);
  }

  public Map<String, String> getProperties() {
    return sampleEvent;
  }

  public Map<String, Double> getMetrics() {
    return metrics;
  }
}
