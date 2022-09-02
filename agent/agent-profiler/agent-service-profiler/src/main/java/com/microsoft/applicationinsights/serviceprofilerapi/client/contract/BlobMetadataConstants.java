// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.client.contract;

/**
 * {@code BlobMetadataConstants} class defines names for well-known properties are set on profiler
 * trace file blobs.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public final class BlobMetadataConstants {
  public static final String DATA_CUBE_META_NAME = "spDataCube";
  public static final String MACHINE_NAME_META_NAME = "spMachineName";
  public static final String START_TIME_META_NAME = "spTraceStartTime";
  public static final String PROGRAMMING_LANGUAGE_META_NAME = "spProgrammingLanguage";
  public static final String OS_PLATFORM_META_NAME = "spOSPlatform";
  public static final String TRACE_FILE_FORMAT_META_NAME = "spTraceFileFormat";
  public static final String ROLE_NAME_META_NAME = "RoleName";

  private BlobMetadataConstants() {}
}
