// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

import com.google.auto.value.AutoValue;
import java.io.File;
import java.util.UUID;

/**
 * {@code UploadContext} class represents parameters for trace file upload operation.
 *
 * <p>This class is intended for internal Java profiler use.
 */
@AutoValue
abstract class UploadContext {

  abstract String getMachineName();

  abstract File getTraceFile();

  abstract long getSessionId();

  abstract UUID getDataCube();

  abstract UUID getProfileId();

  abstract String getFileFormat();

  abstract String getExtension();

  static Builder builder() {
    return new AutoValue_UploadContext.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setMachineName(String machineName);

    abstract Builder setTraceFile(File traceFile);

    abstract Builder setSessionId(long sessionId);

    abstract Builder setDataCube(UUID dataCube);

    abstract Builder setProfileId(UUID profileId);

    abstract Builder setFileFormat(String fileFormat);

    abstract Builder setExtension(String extension);

    abstract UploadContext build();
  }
}
