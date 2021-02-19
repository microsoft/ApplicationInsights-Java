/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static java.util.Collections.emptySet;

import io.opentelemetry.api.trace.Span;
import java.util.AbstractMap;
import java.util.Set;

public class SpanAttributeProperties extends AbstractMap<String, String> {

  private final Span span;

  public SpanAttributeProperties(Span span) {
    this.span = span;
  }

  @Override
  public String put(String key, String value) {
    span.setAttribute(key, value);
    return null;
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return emptySet();
  }
}
