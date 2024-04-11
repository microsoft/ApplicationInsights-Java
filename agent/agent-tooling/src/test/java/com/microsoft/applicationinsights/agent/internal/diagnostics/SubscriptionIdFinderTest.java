// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionIdFinderTest {

  @Nullable private SubscriptionIdFinder subIdFinder;

  private static final Map<String, String> envVars = new HashMap<>();

  @BeforeEach
  void setup() {
    subIdFinder = new SubscriptionIdFinder();
    envVars.clear();
  }

  @AfterEach
  void tearDown() {
    subIdFinder = null;
  }

  @Test
  void happyPathWithValidSubscriptionIdInsideWebsiteOwnerNameVar() {
    envVars.put(SubscriptionIdFinder.WEBSITE_OWNER_NAME_ENV_VAR, "sub-id-123+NOT_SUB_ID");
    String value = subIdFinder.getValue(this::envVarsFunction);
    assertThat(value).isEqualTo("sub-id-123");
  }

  @Test
  void useUnknownWhenEnvVarIsUnset() {
    assertThat(subIdFinder.getValue(this::envVarsFunction)).isEqualTo("unknown");
  }

  @Test
  void useUnknownIfEnvVarHasUnexpectedFormat() {
    envVars.put(SubscriptionIdFinder.WEBSITE_OWNER_NAME_ENV_VAR, "NOT_VALID");
    assertThat(subIdFinder.getValue(this::envVarsFunction)).isEqualTo("unknown");
  }

  @SuppressWarnings("MethodCanBeStatic")
  private String envVarsFunction(String key) {
    return envVars.get(key);
  }
}
