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

package com.microsoft.applicationinsights.agent.internal.wascore;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MetricFilter {

  // OpenTelemetry Collector also supports include
  // but we aren't adding this support, at least not yet, since it implicitly excludes everything
  // else
  // which is a bit confusing currently at least with mix of built-in perf counters, jmx metrics
  // and micrometer metrics
  // we may revisit include in the future after metrics are stabilized if there is customer need
  public final IncludeExclude exclude;

  public MetricFilter(Configuration.ProcessorConfig metricFilterConfiguration) {
    this.exclude = new IncludeExclude(metricFilterConfiguration.exclude);
  }

  boolean matches(String metricName) {
    return !exclude.matches(metricName);
  }

  public static class IncludeExclude {
    public final Configuration.MatchType matchType;
    public final Set<String> metricNames;
    public final List<Pattern> metricNamePatterns;

    public IncludeExclude(Configuration.ProcessorIncludeExclude includeExcludeConfiguration) {
      this.matchType = includeExcludeConfiguration.matchType;
      switch (matchType) {
        case STRICT:
          this.metricNames = new HashSet<>(includeExcludeConfiguration.metricNames);
          this.metricNamePatterns = Collections.emptyList();
          break;
        case REGEXP:
          this.metricNames = Collections.emptySet();
          this.metricNamePatterns = new ArrayList<>();
          for (String metricName : includeExcludeConfiguration.metricNames) {
            // these patterns have already been validated in
            // Configuration.MetricFilterConfig.validate()
            this.metricNamePatterns.add(Pattern.compile(metricName));
          }
          break;
        default:
          throw new AssertionError("Unexpected match type: " + matchType);
      }
    }

    boolean matches(String metricName) {
      switch (matchType) {
        case STRICT:
          return metricNames.contains(metricName);
        case REGEXP:
          for (Pattern metricNamePattern : metricNamePatterns) {
            if (!metricNamePattern.matcher(metricName).matches()) {
              return false;
            }
          }
          return true;
      }
      throw new AssertionError("Unexpected match type: " + matchType);
    }
  }
}
