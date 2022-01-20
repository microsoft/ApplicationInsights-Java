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

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverrideAttribute;
import com.microsoft.applicationinsights.agent.internal.exporter.Exporter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

// TODO find a better name for this class (and MatcherGroup too)
class SamplingOverrides {

  private final List<MatcherGroup> matcherGroups;

  SamplingOverrides(List<SamplingOverride> overrides) {
    matcherGroups = new ArrayList<>();
    for (SamplingOverride override : overrides) {
      matcherGroups.add(new MatcherGroup(override));
    }
  }

  @Nullable
  MatcherGroup getOverride(SpanKind spanKind, Attributes attributes) {
    LazyHttpUrl lazyHttpUrl = new LazyHttpUrl(attributes);
    for (MatcherGroup matcherGroups : matcherGroups) {
      if (matcherGroups.matches(spanKind, attributes, lazyHttpUrl)) {
        return matcherGroups;
      }
    }
    return null;
  }

  static SamplingResult getRecordAndSampleAndOverwriteTraceState(double samplingPercentage) {
    return new TraceStateUpdatingSamplingResult(
        SamplingDecision.RECORD_AND_SAMPLE, toRoundedString(samplingPercentage), true);
  }

  static SamplingResult getRecordAndSampleAndAddTraceStateIfMissing(double samplingPercentage) {
    return new TraceStateUpdatingSamplingResult(
        SamplingDecision.RECORD_AND_SAMPLE, toRoundedString(samplingPercentage), false);
  }

  // TODO write test for
  //  * 33.33333333333
  //  * 66.66666666666
  //  * 1.123456
  //  * 50.0
  //  * 1.0
  //  * 0
  //  * 0.001
  //  * 0.000001
  // 5 digit of precision, and remove any trailing zeros beyond the decimal point
  private static String toRoundedString(double percentage) {
    BigDecimal bigDecimal = new BigDecimal(percentage);
    bigDecimal = bigDecimal.round(new MathContext(5));
    String formatted = bigDecimal.toString();
    double dv = bigDecimal.doubleValue();
    if (dv > 0 && dv < 1) {
      while (formatted.endsWith("0")) {
        formatted = formatted.substring(0, formatted.length() - 1);
      }
    }
    return formatted;
  }

  private static final class TraceStateUpdatingSamplingResult implements SamplingResult {

    private final SamplingDecision decision;
    private final String samplingPercentage;
    private final TraceState traceState;
    private final boolean overwriteExisting;

    private TraceStateUpdatingSamplingResult(
        SamplingDecision decision, String samplingPercentage, boolean overwriteExisting) {
      this.decision = decision;
      this.samplingPercentage = samplingPercentage;
      this.overwriteExisting = overwriteExisting;
      traceState =
          TraceState.builder()
              .put(TelemetryUtil.SAMPLING_PERCENTAGE_TRACE_STATE, samplingPercentage)
              .build();
    }

    @Override
    public SamplingDecision getDecision() {
      return decision;
    }

    @Override
    public Attributes getAttributes() {
      return Attributes.empty();
    }

    @Override
    public TraceState getUpdatedTraceState(TraceState parentTraceState) {
      if (parentTraceState.isEmpty()) {
        return traceState;
      }
      String existingSamplingPercentage =
          parentTraceState.get(TelemetryUtil.SAMPLING_PERCENTAGE_TRACE_STATE);
      if (samplingPercentage.equals(existingSamplingPercentage)) {
        return parentTraceState;
      }
      if (existingSamplingPercentage != null && !overwriteExisting) {
        return parentTraceState;
      }
      return parentTraceState
          .toBuilder()
          .put(TelemetryUtil.SAMPLING_PERCENTAGE_TRACE_STATE, samplingPercentage)
          .build();
    }
  }

  static class MatcherGroup {
    @Nullable private final SpanKind spanKind;
    private final List<TempPredicate> predicates;
    private final double percentage;
    private final SamplingResult recordAndSampleAndOverwriteTraceState;

    private MatcherGroup(SamplingOverride override) {
      spanKind = override.spanKind != null ? override.spanKind.otelSpanKind : null;
      predicates = new ArrayList<>();
      for (SamplingOverrideAttribute attribute : override.attributes) {
        predicates.add(toPredicate(attribute));
      }
      percentage = override.percentage;
      recordAndSampleAndOverwriteTraceState =
          SamplingOverrides.getRecordAndSampleAndOverwriteTraceState(percentage);
    }

    double getPercentage() {
      return percentage;
    }

    SamplingResult getRecordAndSampleAndOverwriteTraceState() {
      return recordAndSampleAndOverwriteTraceState;
    }

    private boolean matches(SpanKind spanKind, Attributes attributes, LazyHttpUrl lazyHttpUrl) {
      if (this.spanKind != null && !this.spanKind.equals(spanKind)) {
        return false;
      }
      for (TempPredicate predicate : predicates) {
        if (!predicate.test(attributes, lazyHttpUrl)) {
          return false;
        }
      }
      return true;
    }

    private static TempPredicate toPredicate(SamplingOverrideAttribute attribute) {
      if (attribute.matchType == MatchType.STRICT) {
        return new StrictMatcher(attribute.key, attribute.value);
      } else if (attribute.matchType == MatchType.REGEXP) {
        return new RegexpMatcher(attribute.key, attribute.value);
      } else if (attribute.matchType == null) {
        return new KeyOnlyMatcher(attribute.key);
      } else {
        throw new IllegalStateException("Unexpected match type: " + attribute.matchType);
      }
    }
  }

  private static class StrictMatcher implements TempPredicate {
    private final AttributeKey<String> key;
    private final String value;

    private StrictMatcher(String key, String value) {
      this.key = AttributeKey.stringKey(key);
      this.value = value;
    }

    @Override
    public boolean test(Attributes attributes, LazyHttpUrl lazyHttpUrl) {
      String val = attributes.get(key);
      if (val == null && key.getKey().equals(SemanticAttributes.HTTP_URL.getKey())) {
        val = lazyHttpUrl.get();
      }
      return value.equals(val);
    }
  }

  private static class RegexpMatcher implements TempPredicate {
    private final AttributeKey<String> key;
    private final Pattern value;

    private RegexpMatcher(String key, String value) {
      this.key = AttributeKey.stringKey(key);
      this.value = Pattern.compile(value);
    }

    @Override
    public boolean test(Attributes attributes, LazyHttpUrl lazyHttpUrl) {
      String val = attributes.get(key);
      if (val == null && key.getKey().equals(SemanticAttributes.HTTP_URL.getKey())) {
        val = lazyHttpUrl.get();
      }
      return val != null && value.matcher(val).matches();
    }
  }

  private static class KeyOnlyMatcher implements TempPredicate {
    private final AttributeKey<String> key;

    private KeyOnlyMatcher(String key) {
      this.key = AttributeKey.stringKey(key);
    }

    @Override
    public boolean test(Attributes attributes, LazyHttpUrl lazyHttpUrl) {
      String val = attributes.get(key);
      if (val == null && key.getKey().equals(SemanticAttributes.HTTP_URL.getKey())) {
        val = lazyHttpUrl.get();
      }
      return val != null;
    }
  }

  // this is temporary until semantic attributes stabilize and we make breaking change
  private static class LazyHttpUrl {
    private final Attributes attributes;
    private boolean initialized;
    @Nullable private String value;

    private LazyHttpUrl(Attributes attributes) {
      this.attributes = attributes;
    }

    private String get() {
      if (!initialized) {
        value = Exporter.getHttpUrlFromServerSpan(attributes);
        initialized = true;
      }
      return value;
    }
  }

  // this is temporary until semantic attributes stabilize and we make breaking change
  // then can use java.util.functions.Predicate<Attributes>
  private interface TempPredicate {
    boolean test(Attributes attributes, LazyHttpUrl lazyHttpUrl);
  }
}
