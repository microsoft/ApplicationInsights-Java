package com.microsoft.applicationinsights.agent.internal.profiler.config;

@FunctionalInterface
public interface ProfilerConfigurationUpdateListener {

  void onUpdate(ProfilerConfiguration config);
}
