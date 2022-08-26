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

import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverrideAttribute;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

// TODO find a better name for this class (and MatcherGroup too)
public class SamplingOverrides {

  private final List<MatcherGroup> matcherGroups;

  public SamplingOverrides(List<SamplingOverride> overrides) {
    matcherGroups = new ArrayList<>();
    for (SamplingOverride override : overrides) {
      matcherGroups.add(new MatcherGroup(override));
    }
  }

  @Nullable
  public Sampler getOverride(boolean standaloneTelemetry, Attributes attributes) {
    LazyHttpUrl lazyHttpUrl = new LazyHttpUrl(attributes);
    for (MatcherGroup matcherGroups : matcherGroups) {
      if (matcherGroups.matches(standaloneTelemetry, attributes, lazyHttpUrl)) {
        return matcherGroups.getSampler();
      }
    }
    return null;
  }

  // used to do sampling inside the log exporter
  @Nullable
  public Double getOverridePercentage(boolean standaloneTelemetry, Attributes attributes) {
    for (MatcherGroup matcherGroups : matcherGroups) {
      if (matcherGroups.matches(standaloneTelemetry, attributes, null)) {
        return matcherGroups.getPercentage();
      }
    }
    return null;
  }

  private static class MatcherGroup {
    private final boolean includeStandaloneTelemetry;
    private final List<TempPredicate> predicates;
    private final Sampler sampler;
    private final SamplingPercentage samplingPercentage;

    private MatcherGroup(SamplingOverride override) {
      includeStandaloneTelemetry = override.includeStandaloneTelemetry;
      predicates = new ArrayList<>();
      for (SamplingOverrideAttribute attribute : override.attributes) {
        predicates.add(toPredicate(attribute));
      }
      samplingPercentage = SamplingPercentage.fixed(override.percentage);
      sampler = new AiSampler(samplingPercentage, false);
    }

    Sampler getSampler() {
      return sampler;
    }

    double getPercentage() {
      return samplingPercentage.get();
    }

    private boolean matches(
        boolean standaloneTelemetry, Attributes attributes, @Nullable LazyHttpUrl lazyHttpUrl) {
      if (standaloneTelemetry && !this.includeStandaloneTelemetry) {
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
        if (isHttpHeaderAttribute(attribute)) {
          return new StrictArrayContainsMatcher(attribute.key, attribute.value);
        } else {
          return new StrictMatcher(attribute.key, attribute.value);
        }
      } else if (attribute.matchType == MatchType.REGEXP) {
        if (isHttpHeaderAttribute(attribute)) {
          return new RegexpArrayContainsMatcher(attribute.key, attribute.value);
        } else {
          return new RegexpMatcher(attribute.key, attribute.value);
        }
      } else if (attribute.matchType == null) {
        return new KeyOnlyMatcher(attribute.key);
      } else {
        throw new IllegalStateException("Unexpected match type: " + attribute.matchType);
      }
    }

    private static boolean isHttpHeaderAttribute(SamplingOverrideAttribute attribute) {
      // note that response headers are not typically available for sampling
      return attribute.key.startsWith("http.request.header.")
          || attribute.key.startsWith("http.response.header.");
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

  private static class StrictArrayContainsMatcher implements TempPredicate {
    private final AttributeKey<List<String>> key;
    private final String value;

    private StrictArrayContainsMatcher(String key, String value) {
      this.key = AttributeKey.stringArrayKey(key);
      this.value = value;
    }

    @Override
    public boolean test(Attributes attributes, LazyHttpUrl lazyHttpUrl) {
      List<String> val = attributes.get(key);
      return val != null && val.contains(value);
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
    public boolean test(Attributes attributes, @Nullable LazyHttpUrl lazyHttpUrl) {
      String val = attributes.get(key);
      if (val == null
          && key.getKey().equals(SemanticAttributes.HTTP_URL.getKey())
          && lazyHttpUrl != null) {
        val = lazyHttpUrl.get();
      }
      return val != null && value.matcher(val).matches();
    }
  }

  private static class RegexpArrayContainsMatcher implements TempPredicate {
    private final AttributeKey<List<String>> key;
    private final Pattern value;

    private RegexpArrayContainsMatcher(String key, String value) {
      this.key = AttributeKey.stringArrayKey(key);
      this.value = Pattern.compile(value);
    }

    @Override
    public boolean test(Attributes attributes, @Nullable LazyHttpUrl lazyHttpUrl) {
      List<String> val = attributes.get(key);
      if (val == null) {
        return false;
      }
      for (String v : val) {
        if (value.matcher(v).matches()) {
          return true;
        }
      }
      return false;
    }
  }

  private static class KeyOnlyMatcher implements TempPredicate {
    private final AttributeKey<String> key;

    private KeyOnlyMatcher(String key) {
      this.key = AttributeKey.stringKey(key);
    }

    @Override
    public boolean test(Attributes attributes, @Nullable LazyHttpUrl lazyHttpUrl) {
      String val = attributes.get(key);
      if (val == null
          && key.getKey().equals(SemanticAttributes.HTTP_URL.getKey())
          && lazyHttpUrl != null) {
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
        value = SpanDataMapper.getHttpUrlFromServerSpan(attributes);
        initialized = true;
      }
      return value;
    }
  }

  // this is temporary until semantic attributes stabilize and we make breaking change
  // then can use java.util.functions.Predicate<Attributes>
  private interface TempPredicate {
    boolean test(Attributes attributes, @Nullable LazyHttpUrl lazyHttpUrl);
  }
}
