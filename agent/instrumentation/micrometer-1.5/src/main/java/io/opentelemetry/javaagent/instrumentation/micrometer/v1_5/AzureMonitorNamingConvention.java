// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.v1_5;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.lang.Nullable;
import java.util.regex.Pattern;

public class AzureMonitorNamingConvention implements NamingConvention {

  private static final Pattern NAME_AND_TAG_KEY_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-]");

  private final NamingConvention delegate;

  public AzureMonitorNamingConvention() {
    this(NamingConvention.snakeCase);
  }

  public AzureMonitorNamingConvention(NamingConvention delegate) {
    this.delegate = delegate;
  }

  @Override
  public String name(String name, Meter.Type type, @Nullable String baseUnit) {
    return NAME_AND_TAG_KEY_PATTERN.matcher(delegate.name(name, type, baseUnit)).replaceAll("_");
  }

  @Override
  public String tagKey(String key) {
    return NAME_AND_TAG_KEY_PATTERN.matcher(delegate.tagKey(key)).replaceAll("_");
  }
}
