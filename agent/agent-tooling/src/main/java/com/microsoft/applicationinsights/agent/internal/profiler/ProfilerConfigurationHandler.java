// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;

/** A service that is able to apply profiler configuration parameters. */
public interface ProfilerConfigurationHandler {
  void updateConfiguration(ProfilerConfiguration newConfig);
}
