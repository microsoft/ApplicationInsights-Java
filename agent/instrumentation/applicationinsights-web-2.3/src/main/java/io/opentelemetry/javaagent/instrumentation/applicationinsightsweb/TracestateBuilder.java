// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import java.util.function.BiConsumer;

public class TracestateBuilder implements BiConsumer<String, String> {

  private final StringBuilder stringBuilder = new StringBuilder();

  @Override
  public void accept(String key, String value) {
    if (stringBuilder.length() != 0) {
      stringBuilder.append(',');
    }
    stringBuilder.append(key).append('=').append(value);
  }

  @Override
  public String toString() {
    return stringBuilder.toString();
  }
}
