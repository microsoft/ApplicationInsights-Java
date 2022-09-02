// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.squareup.moshi.Moshi;
import java.util.Map;

public class MoshiJsonFormatter implements JsonFormatter {

  // only used in tests
  private boolean prettyPrint;

  @Override
  public String toJsonString(Map m) {
    Moshi moshi = new Moshi.Builder().build();
    if (prettyPrint) {
      return moshi.adapter(Map.class).indent("  ").toJson(m);
    } else {
      return moshi.adapter(Map.class).toJson(m);
    }
  }

  // only used in tests
  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }
}
