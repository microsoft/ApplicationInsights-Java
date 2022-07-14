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

package com.microsoft.applicationinsights.alerting.analysis.filter;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** Filters span data based on its name. */
public abstract class AlertRequestFilter implements Predicate<String> {

  /** Filter that applies a regex to the span name. */
  public static class RegexSpanNameFilter extends AlertRequestFilter {

    private final Pattern pattern;

    public RegexSpanNameFilter(String value) {
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
