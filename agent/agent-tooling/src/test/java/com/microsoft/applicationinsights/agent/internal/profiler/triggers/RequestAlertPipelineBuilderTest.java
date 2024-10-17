// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonWriter;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.testutil.TestTimeSource;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class RequestAlertPipelineBuilderTest {

  @SuppressWarnings("DefaultCharset")
  @Test
  public void configurationIsCorrectlyDuplicated() throws IOException {
    Configuration.RequestTrigger expectedRequesttrigger = new Configuration.RequestTrigger();
    expectedRequesttrigger.filter.type = Configuration.RequestFilterType.NAME_REGEX;
    expectedRequesttrigger.filter.value = "foo.*";
    expectedRequesttrigger.threshold.value = 0.75f;

    TestTimeSource timeSource = new TestTimeSource();
    timeSource.setNow(Instant.EPOCH);

    AlertingConfig.RequestTrigger config =
        RequestAlertPipelineBuilder.buildRequestTriggerConfiguration(expectedRequesttrigger);

    String alertingConfigStr;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonWriter writer = JsonProviders.createWriter(outputStream)) {
      config.toJson(writer).flush();
      alertingConfigStr = outputStream.toString();
    }
    AlertingConfig.RequestTrigger actualAlertingConfig;
    try (JsonReader reader = JsonProviders.createReader(alertingConfigStr)) {
      actualAlertingConfig = AlertingConfig.RequestTrigger.fromJson(reader);
    }

    Assertions.assertEquals(expectedRequesttrigger.name, actualAlertingConfig.name);
    Assertions.assertEquals(expectedRequesttrigger.type.name(), actualAlertingConfig.type.name());
    Assertions.assertEquals(
        expectedRequesttrigger.filter.type.name(), actualAlertingConfig.filter.type.name());
    Assertions.assertEquals(expectedRequesttrigger.filter.value, actualAlertingConfig.filter.value);
    Assertions.assertEquals(
        expectedRequesttrigger.aggregation.type.name(),
        actualAlertingConfig.aggregation.type.name());
    Assertions.assertEquals(
        expectedRequesttrigger.threshold.type.name(), actualAlertingConfig.threshold.type.name());
    Assertions.assertEquals(
        expectedRequesttrigger.threshold.value, actualAlertingConfig.threshold.value);
    Assertions.assertEquals(
        expectedRequesttrigger.throttling.type.name(), actualAlertingConfig.throttling.type.name());
    Assertions.assertEquals(
        expectedRequesttrigger.throttling.value, actualAlertingConfig.throttling.value);
    Assertions.assertEquals(
        expectedRequesttrigger.profileDuration, actualAlertingConfig.profileDuration);
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
