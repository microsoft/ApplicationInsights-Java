// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.profiler;

/** Definition of a service that gives access to a profiler. */
public interface ProfilerService {
  Profiler getProfiler();
}
