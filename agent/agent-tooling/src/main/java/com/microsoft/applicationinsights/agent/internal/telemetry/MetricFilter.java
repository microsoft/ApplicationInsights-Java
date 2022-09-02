// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static java.util.Arrays.asList;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.perfcounter.MetricNames;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MetricFilter {

  private static final Set<String> NON_FILTERABLE_METRIC_NAMES =
      new HashSet<>(
          asList(
              MetricNames.TOTAL_CPU_PERCENTAGE,
              MetricNames.PROCESS_CPU_PERCENTAGE,
              MetricNames.PROCESS_CPU_PERCENTAGE_NORMALIZED,
              MetricNames.PROCESS_MEMORY,
              MetricNames.TOTAL_MEMORY,
              MetricNames.PROCESS_IO));

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

  public static boolean shouldSkip(String metricName, List<MetricFilter> metricFilters) {
    if (!MetricFilter.NON_FILTERABLE_METRIC_NAMES.contains(metricName)) {
      for (MetricFilter metricFilter : metricFilters) {
        if (!metricFilter.matches(metricName)) {
          // user configuration filtered out this metric name
          return true;
        }
      }
    }
    return false;
  }
}
