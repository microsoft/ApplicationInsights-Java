// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.SpanDataMapper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverrideAttribute;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO find a better name for this class (and MatcherGroup too)
public class SamplingOverrides {

  private static final Logger logger = LoggerFactory.getLogger(SamplingOverrides.class);
  private final List<MatcherGroup> matcherGroups;

  public SamplingOverrides(List<SamplingOverride> overrides) {
    matcherGroups = new ArrayList<>();
    for (SamplingOverride override : overrides) {
      matcherGroups.add(new MatcherGroup(override));
    }
  }

  @Nullable
  public AiFixedPercentageSampler getOverride(Attributes attributes) {
    LazyHttpUrl lazyHttpUrl = new LazyHttpUrl(attributes);
    LazyHttpTarget lazyHttpTarget = new LazyHttpTarget(attributes);
    for (MatcherGroup matcherGroups : matcherGroups) {
      if (matcherGroups.matches(attributes, lazyHttpUrl, lazyHttpTarget)) {
        return matcherGroups.getSampler();
      }
    }
    return null;
  }

  private static class MatcherGroup {
    private final List<TempPredicate> predicates;
    private final AiFixedPercentageSampler sampler;

    private MatcherGroup(SamplingOverride override) {
      predicates = new ArrayList<>();
      for (SamplingOverrideAttribute attribute : override.attributes) {
        TempPredicate predicate = toPredicate(attribute);
        if (predicate != null) {
          predicates.add(predicate);
        }
      }
      sampler = AiFixedPercentageSampler.create(override.percentage);
    }

    AiFixedPercentageSampler getSampler() {
      return sampler;
    }

    private boolean matches(
        Attributes attributes,
        @Nullable LazyHttpUrl lazyHttpUrl,
        @Nullable LazyHttpTarget lazyHttpTarget) {
      for (TempPredicate predicate : predicates) {
        if (!predicate.test(attributes, lazyHttpUrl, lazyHttpTarget)) {
          return false;
        }
      }
      return true;
    }

    static String getValueIncludingThreadName(
        Attributes attributes, AttributeKey<String> attributeKey) {
      if (attributeKey.getKey().equals(ThreadIncubatingAttributes.THREAD_NAME.getKey())) {
        return Thread.currentThread().getName();
      } else {
        return attributes.get(attributeKey);
      }
    }

    @Nullable
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
      }
      logger.error("Unexpected match type: " + attribute.matchType);
      return null;
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
    @SuppressWarnings(
        "deprecation") // support deprecated semconv attributes for backwards compatibility
    public boolean test(
        Attributes attributes, LazyHttpUrl lazyHttpUrl, LazyHttpTarget lazyHttpTarget) {
      String val = MatcherGroup.getValueIncludingThreadName(attributes, key);
      if (key.getKey().equals(HttpIncubatingAttributes.HTTP_TARGET.getKey())) {
        val = lazyHttpTarget.get();
      }
      if (val == null && getHttpUrlKeyOldOrStableSemconv(key)) {
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
    public boolean test(
        Attributes attributes, LazyHttpUrl lazyHttpUrl, LazyHttpTarget lazyHttpTarget) {
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
    public boolean test(
        Attributes attributes,
        @Nullable LazyHttpUrl lazyHttpUrl,
        @Nullable LazyHttpTarget lazyHttpTarget) {
      String val = MatcherGroup.getValueIncludingThreadName(attributes, key);
      if (key.getKey().equals(UrlAttributes.URL_PATH.getKey())) {
        val = lazyHttpTarget.get();
      }
      if (val == null && getHttpUrlKeyOldOrStableSemconv(key) && lazyHttpUrl != null) {
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
    public boolean test(
        Attributes attributes,
        @Nullable LazyHttpUrl lazyHttpUrl,
        @Nullable LazyHttpTarget lazyHttpTarget) {
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
    @SuppressWarnings(
        "deprecation") // support deprecated semconv attributes for backwards compatibility
    public boolean test(
        Attributes attributes,
        @Nullable LazyHttpUrl lazyHttpUrl,
        @Nullable LazyHttpTarget lazyHttpTarget) {
      String val = MatcherGroup.getValueIncludingThreadName(attributes, key);
      if (key.getKey().equals(HttpIncubatingAttributes.HTTP_TARGET.getKey())) {
        val = lazyHttpTarget.get();
      }
      if (val == null && getHttpUrlKeyOldOrStableSemconv(key) && lazyHttpUrl != null) {
        val = lazyHttpUrl.get();
      }
      return val != null;
    }
  }

  // this is for backward compatibility with existing sampling override logic
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

  @SuppressWarnings(
      "deprecation") // support deprecated semconv attributes for backwards compatibility
  private static boolean getHttpUrlKeyOldOrStableSemconv(AttributeKey<String> key) {
    String keyString = key.getKey();
    return keyString.equals(HttpIncubatingAttributes.HTTP_URL.getKey())
        || keyString.equals(UrlAttributes.URL_FULL.getKey());
  }

  // this is temporary until semantic attributes stabilize and we make breaking change
  // then can use java.util.functions.Predicate<Attributes>
  private interface TempPredicate {
    boolean test(
        Attributes attributes,
        @Nullable LazyHttpUrl lazyHttpUrl,
        @Nullable LazyHttpTarget lazyHttpTarget);
  }

  // this is for backward compatibility with existing sampling override logic
  // http.target -> url.path and url.query
  private static class LazyHttpTarget {
    private final Attributes attributes;
    private boolean initialized;
    @Nullable private String value;

    private LazyHttpTarget(Attributes attributes) {
      this.attributes = attributes;
    }

    private String get() {
      if (!initialized) {
        String urlQuery = attributes.get(UrlAttributes.URL_QUERY);
        value = attributes.get(UrlAttributes.URL_PATH) + (urlQuery != null ? "?" + urlQuery : "");
        initialized = true;
      }
      return value;
    }
  }
}
