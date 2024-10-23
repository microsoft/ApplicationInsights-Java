// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonWriter;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.testutil.TestTimeSource;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.IOException;
import java.io.StringWriter;
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
  public void configurationIsCorrectlyDuplicated() throws IOException {
    Configuration.RequestTrigger expectedRequestTrigger = new Configuration.RequestTrigger();
    expectedRequestTrigger.filter.type = Configuration.RequestFilterType.NAME_REGEX;
    expectedRequestTrigger.filter.value = "foo.*";
    expectedRequestTrigger.threshold.value = 0.75f;

    TestTimeSource timeSource = new TestTimeSource();
    timeSource.setNow(Instant.EPOCH);

    AlertingConfig.RequestTrigger config =
        RequestAlertPipelineBuilder.buildRequestTriggerConfiguration(expectedRequestTrigger);

    String alertingConfigStr;
    try (StringWriter stringWriter = new StringWriter();
        JsonWriter writer = JsonProviders.createWriter(stringWriter)) {
      config.toJson(writer).flush();
      alertingConfigStr = stringWriter.toString();
    }
    AlertingConfig.RequestTrigger actualAlertingConfig;
    try (JsonReader reader = JsonProviders.createReader(alertingConfigStr)) {
      actualAlertingConfig = AlertingConfig.RequestTrigger.fromJson(reader);
    }

    Assertions.assertEquals(expectedRequestTrigger.name, actualAlertingConfig.name);
    Assertions.assertEquals(expectedRequestTrigger.type.name(), actualAlertingConfig.type.name());
    Assertions.assertEquals(
        expectedRequestTrigger.filter.type.name(), actualAlertingConfig.filter.type.name());
    Assertions.assertEquals(expectedRequestTrigger.filter.value, actualAlertingConfig.filter.value);
    Assertions.assertEquals(
        expectedRequestTrigger.aggregation.type.name(),
        actualAlertingConfig.aggregation.type.name());
    Assertions.assertEquals(
        expectedRequestTrigger.threshold.type.name(), actualAlertingConfig.threshold.type.name());
    Assertions.assertEquals(
        expectedRequestTrigger.threshold.value, actualAlertingConfig.threshold.value);
    Assertions.assertEquals(
        expectedRequestTrigger.throttling.type.name(), actualAlertingConfig.throttling.type.name());
    Assertions.assertEquals(
        expectedRequestTrigger.throttling.value, actualAlertingConfig.throttling.value);
    Assertions.assertEquals(
        expectedRequestTrigger.profileDuration, actualAlertingConfig.profileDuration);
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
                      try (JsonReader reader =
                          JsonProviders.createReader(
                              RequestAlertPipelineBuilderTest.class
                                  .getClassLoader()
                                  .getResourceAsStream(file))) {
                        AlertingConfig.RequestTrigger alertingConfig =
                            AlertingConfig.RequestTrigger.fromJson(reader);
                        Assertions.assertNotNull(alertingConfig);
                      }
                    }))
        .collect(Collectors.toList());
  }
}
