// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

@FunctionalInterface
public interface ProfilerConfigurationUpdateListener {

  void onUpdate(ProfilerConfiguration config);
}
