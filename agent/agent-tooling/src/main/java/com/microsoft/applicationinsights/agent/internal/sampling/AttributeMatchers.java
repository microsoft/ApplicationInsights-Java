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

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.Exporter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AttributeMatchers {

  public static Matcher toPredicate(Configuration.SamplingOverrideAttribute attribute) {
    if (attribute.matchType == Configuration.MatchType.STRICT) {
      return new StrictMatcher(attribute.key, attribute.value);
    } else if (attribute.matchType == Configuration.MatchType.REGEXP) {
      return new RegexpMatcher(attribute.key, attribute.value);
    } else {
      throw new IllegalStateException("Unexpected match type: " + attribute.matchType);
    }
  }

  private static class StrictMatcher implements Matcher {
    private final AttributeKey<String> key;
    private final String value;

    private final boolean httpUrl;

    private StrictMatcher(String key, String value) {
      this.key = AttributeKey.stringKey(key);
      this.value = value;
      httpUrl = key.equals(SemanticAttributes.HTTP_URL.getKey());
    }

    @Override
    public boolean matches(Attributes attributes, LazyHttpUrl lazyHttpUrl) {
      return matches(attributes.get(key), lazyHttpUrl);
    }

    @Override
    public boolean matches(ReadWriteSpan span, LazyHttpUrl lazyHttpUrl) {
      return matches(span.getAttribute(key), lazyHttpUrl);
    }

    private boolean matches(@Nullable String attributeValue, LazyHttpUrl lazyHttpUrl) {
      if (attributeValue == null && httpUrl) {
        attributeValue = lazyHttpUrl.get();
      }
      return attributeValue != null && value.equals(attributeValue);
    }
  }

  private static class RegexpMatcher implements Matcher {
    private final AttributeKey<String> key;
    private final Pattern value;

    private final boolean httpUrl;

    private RegexpMatcher(String key, String value) {
      this.key = AttributeKey.stringKey(key);
      this.value = Pattern.compile(value);
      httpUrl = key.equals(SemanticAttributes.HTTP_URL.getKey());
    }

    @Override
    public boolean matches(Attributes attributes, LazyHttpUrl lazyHttpUrl) {
      return matches(attributes.get(key), lazyHttpUrl);
    }

    @Override
    public boolean matches(ReadWriteSpan span, LazyHttpUrl lazyHttpUrl) {
      return matches(span.getAttribute(key), lazyHttpUrl);
    }

    private boolean matches(@Nullable String attributeValue, LazyHttpUrl lazyHttpUrl) {
      if (attributeValue == null && httpUrl) {
        attributeValue = lazyHttpUrl.get();
      }
      return attributeValue != null && value.matcher(attributeValue).matches();
    }
  }

  // this is temporary until semantic attributes stabilize and we make breaking change
  public static class LazyHttpUrl {
    private final @Nullable Attributes attributes;
    private final @Nullable ReadWriteSpan span;
    private boolean initialized;
    @Nullable private String value;

    public LazyHttpUrl(Attributes attributes) {
      this.attributes = attributes;
      this.span = null;
    }

    public LazyHttpUrl(ReadWriteSpan span) {
      this.span = span;
      this.attributes = null;
    }

    @Nullable
    private String get() {
      if (!initialized) {
        if (attributes != null) {
          value = Exporter.getHttpUrlFromServerSpan(attributes);
        } else if (span != null) {
          value = Exporter.getHttpUrlFromServerSpan(span);
        }
        initialized = true;
      }
      return value;
    }
  }

  public interface Matcher {
    boolean matches(Attributes attributes, LazyHttpUrl lazyHttpUrl);

    boolean matches(ReadWriteSpan attributes, LazyHttpUrl lazyHttpUrl);
  }
}
