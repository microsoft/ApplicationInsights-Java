// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.filter;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** Filters span data based on its name. */
public abstract class AlertRequestFilter implements Predicate<String> {

  /** Filter that applies a regex to the span name. */
  public static class RegexRequestNameFilter extends AlertRequestFilter {

    private final Pattern pattern;

    public RegexRequestNameFilter(String value) {
      pattern = Pattern.compile(value);
    }

    @Override
    public boolean test(@Nullable String spanName) {
      if (spanName == null) {
        return false;
      }
      return pattern.matcher(spanName).matches();
    }
  }

  public static class AcceptAll extends AlertRequestFilter {
    @Override
    public boolean test(String s) {
      return true;
    }
  }
}
