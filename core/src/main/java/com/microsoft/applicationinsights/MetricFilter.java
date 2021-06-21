package com.microsoft.applicationinsights;

import java.util.*;
import java.util.regex.Pattern;

public class MetricFilter {

    // OpenTelemetry Collector also supports include
    // but we aren't adding this support, at least not yet, since it implicitly excludes everything else
    // which is a bit confusing currently at least with mix of built-in perf counters, jmx metrics
    // and micrometer metrics
    // we may revisit include in the future after metrics are stabilized if there is customer need
    public final IncludeExclude exclude;

    public MetricFilter(IncludeExclude exclude) {
        this.exclude = exclude;
    }

    boolean matches(String metricName) {
        return !exclude.matches(metricName);
    }

    public static class IncludeExclude {
        public final MatchType matchType;
        public final Set<String> metricNames;
        public final List<Pattern> metricNamePatterns;

        public IncludeExclude(MatchType matchType, List<String> metricNames) {
            this.matchType = matchType;
            switch (matchType) {
                case STRICT:
                    this.metricNames = new HashSet<>(metricNames);
                    this.metricNamePatterns = Collections.emptyList();
                    break;
                case REGEXP:
                    this.metricNames = Collections.emptySet();
                    this.metricNamePatterns = new ArrayList<>();
                    for (String metricName : metricNames) {
                        // these patterns have already been validated in Configuration.MetricFilterConfig.validate()
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

    public enum MatchType {
        STRICT, REGEXP
    }
}
