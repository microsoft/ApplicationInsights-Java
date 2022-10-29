// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

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

  UploadContext(
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

  String getMachineName() {
    return machineName;
  }

  File getTraceFile() {
    return traceFile;
  }

  long getSessionId() {
    return sessionId;
  }

  UUID getDataCube() {
    return dataCube;
  }

  UUID getProfileId() {
    return profileId;
  }

  String getFileFormat() {
    return fileFormat;
  }

  String getExtension() {
    return extension;
  }
}
