// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.testutil.TestTimeSource;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class RequestAlertPipelineBuilderTest {

  @Test
  public void configurationIsCorrectlyDuplicated() throws JsonProcessingException {
    Configuration.RequestTrigger triggerConfig = new Configuration.RequestTrigger();
    triggerConfig.filter.type = Configuration.RequestFilterType.NAME_REGEX;
    triggerConfig.filter.value = "foo.*";
    triggerConfig.threshold.value = 0.75f;

    TestTimeSource timeSource = new TestTimeSource();
    timeSource.setNow(Instant.EPOCH);

    AlertingConfig.RequestTrigger config =
        RequestAlertPipelineBuilder.buildRequestTriggerConfiguration(triggerConfig);

    ObjectMapper mapper = new ObjectMapper();
    String configurationStr = mapper.writeValueAsString(triggerConfig);
    String alertingConfigStr = mapper.writeValueAsString(config);
    ;

    Assertions.assertEquals(configurationStr, alertingConfigStr);
  }

  @TestFactory
  public List<DynamicTest> configExamplesCanBeParsedToAlertApiConfig() {
    return Stream.of(
            "profile-configs/applicationinsights-trigger-example-1.json",
            "profile-configs/applicationinsights-trigger-example-2.json",
            "profile-configs/applicationinsights-trigger-example-3.json")
        .map(
            file ->
                DynamicTest.dynamicTest(
                    file,
                    () -> {
                      ObjectMapper mapper = new ObjectMapper();
                      JsonNode array =
                          mapper
                              .readTree(
                                  RequestAlertPipelineBuilderTest.class
                                      .getClassLoader()
                                      .getResourceAsStream(file))
                              .get("preview")
                              .get("profiler")
                              .withArray("requestTriggerEndpoints");

                      array.forEach(
                          config -> {
                            try {
                              AlertingConfig.RequestTrigger alertingConfig =
                                  mapper.readValue(
                                      config.toPrettyString(), AlertingConfig.RequestTrigger.class);
                              Assertions.assertNotNull(alertingConfig);
                            } catch (JsonProcessingException e) {
                              Assertions.fail(e);
                            }
                          });
                    }))
        .collect(Collectors.toList());
  }
}
