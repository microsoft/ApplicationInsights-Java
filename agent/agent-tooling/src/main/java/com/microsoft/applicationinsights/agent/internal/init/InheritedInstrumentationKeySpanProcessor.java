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

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.sampling.AttributeMatchers;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InheritedInstrumentationKeySpanProcessor implements SpanProcessor {

  private static final AttributeKey<String> INSTRUMENTATION_KEY_KEY =
      AttributeKey.stringKey("ai.preview.instrumentation_key");

  private final List<MatcherGroup> matcherGroups;

  public InheritedInstrumentationKeySpanProcessor(
      List<Configuration.InstrumentationKeyOverride> instrumentationKeys) {

    matcherGroups =
        instrumentationKeys.stream().map(MatcherGroup::new).collect(Collectors.toList());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Span parentSpan = Span.fromContextOrNull(parentContext);
    if (parentSpan == null) {
      // setting this attribute on the local root span could be moved to Sampler
      MatcherGroup matcherGroup = getMatcherGroup(span);
      if (matcherGroup != null) {
        span.setAttribute(INSTRUMENTATION_KEY_KEY, matcherGroup.instrumentationKey);
      }
      return;
    }
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    String instrumentationKey = parentReadableSpan.getAttribute(INSTRUMENTATION_KEY_KEY);
    if (instrumentationKey != null) {
      span.setAttribute(INSTRUMENTATION_KEY_KEY, instrumentationKey);
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Nullable
  MatcherGroup getMatcherGroup(ReadWriteSpan span) {
    AttributeMatchers.LazyHttpUrl lazyHttpUrl = new AttributeMatchers.LazyHttpUrl(span);
    for (MatcherGroup matcherGroups : matcherGroups) {
      if (matcherGroups.matches(span, lazyHttpUrl)) {
        return matcherGroups;
      }
    }
    return null;
  }

  private static class MatcherGroup {
    private final List<AttributeMatchers.Matcher> matchers;
    private final String instrumentationKey;

    private MatcherGroup(Configuration.InstrumentationKeyOverride override) {
      matchers = new ArrayList<>();
      for (Configuration.SamplingOverrideAttribute attribute : override.attributes) {
        matchers.add(AttributeMatchers.toPredicate(attribute));
      }
      instrumentationKey = override.instrumentationKey;
    }

    private boolean matches(ReadWriteSpan span, AttributeMatchers.LazyHttpUrl lazyHttpUrl) {
      for (AttributeMatchers.Matcher matcher : matchers) {
        if (!matcher.matches(span, lazyHttpUrl)) {
          return false;
        }
      }
      return true;
    }
  }
}
