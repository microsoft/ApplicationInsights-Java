// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.client.uploader;

import java.io.File;
import java.util.UUID;

/**
 * {@code UploadContext} class represents parameters for trace file upload operation.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public class UploadContext {
  private final UUID dataCube;
  private final long sessionId;
  private final File traceFile;
  private final UUID profileId;
  private final String machineName;
  private final String fileFormat;
  private final String extension;

  public UploadContext(
      String machineName,
      UUID dataCube,
      long sessionId,
      File traceFile,
      UUID profileId,
      String fileFormat,
      String extension) {
    this.machineName = machineName;
    this.dataCube = dataCube;
    this.sessionId = sessionId;
    this.traceFile = traceFile;
    this.profileId = profileId;
    this.fileFormat = fileFormat;
    this.extension = extension;
  }

  public String getMachineName() {
    return machineName;
  }

  public File getTraceFile() {
    return traceFile;
  }

  public long getSessionId() {
    return sessionId;
  }

  public UUID getDataCube() {
    return dataCube;
  }

  public UUID getProfileId() {
    return profileId;
  }

  public String getFileFormat() {
    return fileFormat;
  }

  public String getExtension() {
    return extension;
  }
}
