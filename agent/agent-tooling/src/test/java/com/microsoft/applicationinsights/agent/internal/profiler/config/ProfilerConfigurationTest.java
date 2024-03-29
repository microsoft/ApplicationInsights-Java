// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProfilerConfigurationTest {

  @Test
  void hasBeenConfiguredDetectsDefaultDate() throws JsonProcessingException {
    String configStr =
        "{\"id\":\"an-id\",\"lastModified\":\"0001-01-01T00:00:00+00:00\",\"enabledLastModified\":\"0001-01-01T00:00:00+00:00\",\"enabled\":true,\"collectionPlan\":\"\",\"cpuTriggerConfiguration\":\"--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true\",\"memoryTriggerConfiguration\":\"--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true\",\"defaultConfiguration\":null,\"agentConcurrency\":0}";

    ProfilerConfiguration config = parseConfig(configStr);

    Assertions.assertFalse(config.hasBeenConfigured());
  }

  @Test
  void parsingCopesWithNullCpuTriggerConfiguration() throws JsonProcessingException {
    String configStr =
        "{\"id\":\"an-id\",\"lastModified\":\"0001-01-01T00:00:00+00:00\",\"enabledLastModified\":\"0001-01-01T00:00:00+00:00\",\"enabled\":true,\"collectionPlan\":\"\",\"cpuTriggerConfiguration\":null,\"memoryTriggerConfiguration\":\"--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true\",\"defaultConfiguration\":null,\"agentConcurrency\":0}";

    ProfilerConfiguration config = parseConfig(configStr);

    Assertions.assertTrue(config.getCpuTriggerConfiguration() == null);
  }

  @Test
  void parsingCopesWithNullMemoryTriggerConfiguration() throws JsonProcessingException {
    String configStr =
        "{\"id\":\"an-id\",\"lastModified\":\"0001-01-01T00:00:00+00:00\",\"enabledLastModified\":\"0001-01-01T00:00:00+00:00\",\"enabled\":true,\"collectionPlan\":\"\",\"cpuTriggerConfiguration\":\"--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true\",\"memoryTriggerConfiguration\":null,\"defaultConfiguration\":null,\"agentConcurrency\":0}";

    ProfilerConfiguration config = parseConfig(configStr);

    Assertions.assertTrue(config.getMemoryTriggerConfiguration() == null);
  }

  private static ProfilerConfiguration parseConfig(String configStr)
      throws JsonProcessingException {
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    ProfilerConfiguration config = mapper.readValue(configStr, ProfilerConfiguration.class);
    return config;
  }
}
