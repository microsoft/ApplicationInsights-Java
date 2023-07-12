// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.log;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JacksonJsonFormatter implements JsonFormatter {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String toJsonString(Map m) throws JsonProcessingException {
    return mapper.writeValueAsString(m);
  }
}
