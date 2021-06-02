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

package com.microsoft.applicationinsights.serviceprofilerapi.client.contract;

/**
 * {@code BlobMetadataConstants} class defines names for well-known properties are set on profiler
 * trace file blobs.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public class BlobMetadataConstants {
  public static final String DATA_CUBE_META_NAME = "spDataCube";
  public static final String MACHINE_NAME_META_NAME = "spMachineName";
  public static final String START_TIME_META_NAME = "spTraceStartTime";
  public static final String PROGRAMMING_LANGUAGE_META_NAME = "spProgrammingLanguage";
  public static final String OS_PLATFORM_META_NAME = "spOSPlatform";
  public static final String TRACE_FILE_FORMAT_META_NAME = "spTraceFileFormat";
  public static final String ROLE_NAME_META_NAME = "RoleName";
}
