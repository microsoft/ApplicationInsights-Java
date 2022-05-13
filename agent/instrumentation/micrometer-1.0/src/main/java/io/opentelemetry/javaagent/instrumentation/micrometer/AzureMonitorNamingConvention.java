/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

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
