// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.lang.Nullable;
import java.util.regex.Pattern;

/**
 * Naming convention to push metrics to Azure Monitor.
 *
 * @author Dhaval Doshi
 * @since 1.1.0
 */
public class AzureMonitorNamingConvention implements NamingConvention {
  private static final Pattern NAME_AND_TAG_KEY_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-]");

  private final NamingConvention delegate;

  public AzureMonitorNamingConvention() {
    this(NamingConvention.snakeCase);
  }

  public AzureMonitorNamingConvention(NamingConvention delegate) {
    this.delegate = delegate;
  }

  /** Trimming takes place in App Insights core SDK. */
  @Override
  public String name(String name, Meter.Type type, @Nullable String baseUnit) {
    return NAME_AND_TAG_KEY_PATTERN.matcher(delegate.name(name, type, baseUnit)).replaceAll("_");
  }

  @Override
  public String tagKey(String key) {
    return NAME_AND_TAG_KEY_PATTERN.matcher(delegate.tagKey(key)).replaceAll("_");
  }
}
