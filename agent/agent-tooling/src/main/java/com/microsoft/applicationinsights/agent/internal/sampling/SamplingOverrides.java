package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.SamplingOverrideAttribute;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

// TODO find a better name for this class (and MatcherGroup too)
class SamplingOverrides {

    private final List<MatcherGroup> matcherGroups;

    SamplingOverrides(List<SamplingOverride> overrides) {
        matcherGroups = new ArrayList<>();
        for (SamplingOverride override : overrides) {
            matcherGroups.add(new MatcherGroup(override));
        }
    }

    MatcherGroup getOverride(Attributes attributes) {
        for (MatcherGroup matcherGroups : matcherGroups) {
            if (matcherGroups.matches(attributes)) {
                return matcherGroups;
            }
        }
        return null;
    }

    static SamplingResult getRecordAndSampleResult(double percentage) {
        Attributes alwaysOnAttributes;
        if (percentage != 100) {
            alwaysOnAttributes = Attributes.of(Exporter.AI_SAMPLING_PERCENTAGE_KEY, percentage);
        } else {
            // the exporter assumes 100 when the AI_SAMPLING_PERCENTAGE_KEY attribute is not present
            alwaysOnAttributes = Attributes.empty();
        }
        return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE, alwaysOnAttributes);
    }

    static class MatcherGroup {
        private final List<Predicate<Attributes>> predicates;
        private final double percentage;
        private final SamplingResult recordAndSampleResult;

        private MatcherGroup(SamplingOverride override) {
            predicates = new ArrayList<>();
            for (SamplingOverrideAttribute attribute : override.attributes) {
                predicates.add(toPredicate(attribute));
            }
            percentage = override.percentage;
            recordAndSampleResult = SamplingOverrides.getRecordAndSampleResult(percentage);
        }

        double getPercentage() {
            return percentage;
        }

        SamplingResult getRecordAndSampleResult() {
            return recordAndSampleResult;
        }

        private boolean matches(Attributes attributes) {
            for (Predicate<Attributes> predicate : predicates) {
                if (!predicate.test(attributes)) {
                    return false;
                }
            }
            return true;
        }

        private static Predicate<Attributes> toPredicate(SamplingOverrideAttribute attribute) {
            if (attribute.matchType == MatchType.strict) {
                return new StrictMatcher(attribute.key, attribute.value);
            } else if (attribute.matchType == MatchType.regexp) {
                return new RegexpMatcher(attribute.key, attribute.value);
            } else {
                throw new IllegalStateException("Unexpected match type: " + attribute.matchType);
            }
        }
    }

    private static class StrictMatcher implements Predicate<Attributes> {
        private final AttributeKey<String> key;
        private final String value;

        private StrictMatcher(String key, String value) {
            this.key = AttributeKey.stringKey(key);
            this.value = value;
        }

        @Override
        public boolean test(Attributes attributes) {
            String val = attributes.get(key);
            return value.equals(val);
        }
    }

    private static class RegexpMatcher implements Predicate<Attributes> {
        private final AttributeKey<String> key;
        private final Pattern value;

        private RegexpMatcher(String key, String value) {
            this.key = AttributeKey.stringKey(key);
            this.value = Pattern.compile(value);
        }

        @Override
        public boolean test(Attributes attributes) {
            String val = attributes.get(key);
            return val != null && value.matcher(val).matches();
        }
    }
}
