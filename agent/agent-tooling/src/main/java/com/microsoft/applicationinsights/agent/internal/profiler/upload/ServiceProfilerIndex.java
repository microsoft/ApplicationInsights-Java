// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Metadata used by service profiler. */
public class ServiceProfilerIndex {

  private final Map<String, String> sampleEvent;
  private final Map<String, Double> metrics;

  private ServiceProfilerIndex(Map<String, String> sampleEvent, Map<String, Double> metrics) {
    this.sampleEvent = sampleEvent;
    this.metrics = metrics;
  }

  public Map<String, String> getProperties() {
    return sampleEvent;
  }

  public Map<String, Double> getMetrics() {
    return metrics;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private static final String SERVICE_PROFILER_SOURCE_PROPERTY_NAME = "Source";
    private static final String SERVICE_PROFILER_FILEID_PROPERTY_NAME = "FileId";
    // visible for testing
    public static final String SERVICE_PROFILER_STAMPID_PROPERTY_NAME = "StampId";
    // visible for testing
    public static final String SERVICE_PROFILER_DATACUBE_PROPERTY_NAME = "DataCube";
    // visible for testing
    public static final String SERVICE_PROFILER_MACHINENAME_PROPERTY_NAME = "MachineName";
    private static final String SERVICE_PROFILER_PROCESSID_PROPERTY_NAME = "ProcessId";
    // visible for testing
    public static final String SERVICE_PROFILER_ETLFILESESSIONID_PROPERTY_NAME = "EtlFileSessionId";
    private static final String SERVICE_PROFILER_OPERATINGSYSTEM_PROPERTY_NAME = "OperatingSystem";
    private static final String SERVICE_PROFILER_AVERAGECPUUSAGE_METRIC_NAME = "AverageCPUUsage";
    private static final String SERVICE_PROFILER_AVERAGE_MEMORY_USAGE_METRIC_NAME =
        "AverageMemoryUsage";
    private static final String SERVICE_PROFILER_ARTIFACT_KIND_NAME = "ArtifactKind";
    private static final String SERVICE_PROFILER_ARTIFACT_ID_NAME = "ArtifactId";
    private static final String SERVICE_PROFILER_EXTENSION_NAME = "Extension";

    private final Map<String, String> sampleEvent = new HashMap<>();
    private final Map<String, Double> metrics = new HashMap<>();

    public Builder setTriggeredBy(String triggeredBy) {
      sampleEvent.put(SERVICE_PROFILER_SOURCE_PROPERTY_NAME, triggeredBy);
      return this;
    }

    public Builder setFileId(String fileId) {
      sampleEvent.put(SERVICE_PROFILER_FILEID_PROPERTY_NAME, fileId);
      return this;
    }

    public Builder setStampId(String stampId) {
      sampleEvent.put(SERVICE_PROFILER_STAMPID_PROPERTY_NAME, stampId);
      return this;
    }

    public Builder setDataCubeId(UUID dataCubeId) {
      sampleEvent.put(SERVICE_PROFILER_DATACUBE_PROPERTY_NAME, dataCubeId.toString());
      return this;
    }

    public Builder setTimeStamp(String timeStamp) {
      sampleEvent.put(SERVICE_PROFILER_ETLFILESESSIONID_PROPERTY_NAME, timeStamp);
      return this;
    }

    public Builder setMachineName(String machineName) {
      sampleEvent.put(SERVICE_PROFILER_MACHINENAME_PROPERTY_NAME, machineName);
      return this;
    }

    public Builder setOs(String os) {
      sampleEvent.put(SERVICE_PROFILER_OPERATINGSYSTEM_PROPERTY_NAME, os);
      return this;
    }

    public Builder setProcessId(String processId) {
      sampleEvent.put(SERVICE_PROFILER_PROCESSID_PROPERTY_NAME, processId);
      return this;
    }

    public Builder setArtifactKind(String artifactKind) {
      sampleEvent.put(SERVICE_PROFILER_ARTIFACT_KIND_NAME, artifactKind);
      return this;
    }

    public Builder setArtifactId(String artifactId) {
      sampleEvent.put(SERVICE_PROFILER_ARTIFACT_ID_NAME, artifactId);
      return this;
    }

    public Builder setExtension(String extension) {
      sampleEvent.put(SERVICE_PROFILER_EXTENSION_NAME, extension);
      return this;
    }

    public Builder setCpuUsage(double cpuUsage) {
      metrics.put(SERVICE_PROFILER_AVERAGECPUUSAGE_METRIC_NAME, cpuUsage);
      return this;
    }

    public Builder setMemoryUsage(double memoryUsage) {
      metrics.put(SERVICE_PROFILER_AVERAGE_MEMORY_USAGE_METRIC_NAME, memoryUsage);
      return this;
    }

    public ServiceProfilerIndex build() {
      return new ServiceProfilerIndex(sampleEvent, metrics);
    }
  }
}
