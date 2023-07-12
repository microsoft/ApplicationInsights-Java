// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JacksonJsonFormatterTests {

  @Nullable private JacksonJsonFormatter formatter;

  @BeforeEach
  void setup() {
    formatter = new JacksonJsonFormatter();
  }

  @AfterEach
  void tearDown() {
    formatter = null;
  }

  @Test
  void formatterSerializesSimpleMap() throws JsonProcessingException {
    Map<String, Object> m = new HashMap<>();
    m.put("s1", "v1");
    m.put("int1", 123);
    m.put("b", true);
    assertThat(formatter.toJsonString(m)).isEqualTo("{\"b\":true,\"int1\":123,\"s1\":\"v1\"}");
  }
}
