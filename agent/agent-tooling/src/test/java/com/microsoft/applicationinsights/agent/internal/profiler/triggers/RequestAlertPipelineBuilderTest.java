// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.testutil.TestTimeSource;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    // Account for serialization differences
    alertingConfigStr =
        alertingConfigStr
            .replaceAll("NAME_REGEX", "name-regex")
            .replaceAll("BREACH_RATIO", "breach-ratio")
            .replaceAll("GREATER_THAN", "greater-than")
            .replaceAll("FIXED_DURATION_COOLDOWN", "fixed-duration-cooldown");

    Assertions.assertEquals(configurationStr, alertingConfigStr);
  }
}
