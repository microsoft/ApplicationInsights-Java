// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.azure.json.JsonOptions;
import com.azure.json.JsonReader;
import com.azure.json.implementation.DefaultJsonReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

  @Test
  public void testAlertDeserialization() {
    try (InputStreamReader json =
        new InputStreamReader(
            ProfilerConfigurationTest.class
                .getClassLoader()
                .getResourceAsStream("profile-configs/sample-alert-config.json"),
            Charset.forName("UTF-8"))) {

      JsonReader reader = DefaultJsonReader.fromReader(json, new JsonOptions());
      ProfilerConfiguration profilerConfiguration = ProfilerConfiguration.fromJson(reader);
      Assertions.assertTrue(profilerConfiguration != null);
      AlertingConfig.RequestTrigger requestTrigger =
          profilerConfiguration.getRequestTriggerConfiguration().get(0);
      Assertions.assertEquals(requestTrigger.getType(), AlertingConfig.RequestTriggerType.LATENCY);
      Assertions.assertEquals(
          requestTrigger.getFilter().getType(), AlertingConfig.RequestFilterType.NAME_REGEX);
      Assertions.assertEquals(requestTrigger.getFilter().getValue(), ".*");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
