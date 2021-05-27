package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AgentProcessor {
    private final @Nullable IncludeExclude include;
    private final @Nullable IncludeExclude exclude;

    public AgentProcessor(@Nullable IncludeExclude include,
                          @Nullable IncludeExclude exclude) {
        this.include = include;
        this.exclude = exclude;
    }

    protected static AttributeProcessor.IncludeExclude getNormalizedIncludeExclude(ProcessorIncludeExclude includeExclude) {
        return includeExclude.matchType == MatchType.strict ? AgentProcessor.StrictIncludeExclude.create(includeExclude) : AgentProcessor.RegexpIncludeExclude.create(includeExclude);
    }

    public @Nullable IncludeExclude getInclude() {
        return include;
    }

    public @Nullable IncludeExclude getExclude() {
        return exclude;
    }


    public static abstract class IncludeExclude {
        // Function to compare span with user provided span names or span patterns
        public abstract boolean isMatch(SpanData span, boolean isLog);

    }

    // ok to have this class cover both spanNames and logNames
    public static class StrictIncludeExclude extends IncludeExclude {
        private final List<ProcessorAttribute> attributes;
        private final List<String> spanNames;
        private final List<String> logNames;

        public StrictIncludeExclude(List<ProcessorAttribute> attributes, List<String> spanNames, List<String> logNames) {
            this.attributes = attributes;
            this.spanNames = spanNames;
            this.logNames = logNames;
        }

        public static StrictIncludeExclude create(ProcessorIncludeExclude includeExclude) {
            List<ProcessorAttribute> attributes = includeExclude.attributes;
            if (attributes == null) {
                attributes = new ArrayList<>();
            }
            List<String> spanNames = includeExclude.spanNames;
            if (spanNames == null) {
                spanNames = new ArrayList<>();
            }
            List<String> logNames = includeExclude.logNames;
            if (logNames == null) {
                logNames = new ArrayList<>();
            }

            return new StrictIncludeExclude(attributes, spanNames, logNames);
        }

        // Function to compare span with user provided span names and log names
        public boolean isMatch(SpanData span, boolean isLog) {

            if(spanNames.isEmpty() && logNames.isEmpty()) {
                // check attributes for both spans and logs
                return this.checkAttributes(span);
            }
            if(isLog) {
               if(logNames.isEmpty()) return false;
               if(!logNames.isEmpty() && !logNames.contains(span.getName())) return false;
            } else {
                if(spanNames.isEmpty()) return false;
                if(!spanNames.isEmpty() && !spanNames.contains(span.getName())) return false;
            }

            return this.checkAttributes(span);
        }



        // Function to compare span with user provided attributes list
        private boolean checkAttributes(SpanData span) {
            for (ProcessorAttribute attribute : attributes) {
                //All of these attributes must match exactly for a match to occur.
                Object existingAttributeValue = span.getAttributes().get(AttributeKey.stringKey(attribute.key));
                // to get the string value
                //existingAttributeValue.toString()
                //String.valueOf(existingAttributeValue);
                if (!(existingAttributeValue instanceof String)) {
                    // user specified key not found
                    return false;
                }
                if (attribute.value != null && !existingAttributeValue.equals(attribute.value)) {
                    // user specified value doesn't match
                    return false;
                }
            }
            // everything matched!!!
            return true;
        }
    }

    public static class RegexpIncludeExclude extends IncludeExclude {

        private final List<Pattern> spanPatterns;
        private final List<Pattern> logPatterns;
        private final Map<AttributeKey<?>, Pattern> attributeValuePatterns;

        public RegexpIncludeExclude(List<Pattern> spanPatterns, List<Pattern> logPatterns, Map<AttributeKey<?>, Pattern> attributeValuePatterns) {
            this.spanPatterns = spanPatterns;
            this.logPatterns = logPatterns;
            this.attributeValuePatterns = attributeValuePatterns;
        }

        public static RegexpIncludeExclude create(ProcessorIncludeExclude includeExclude) {
            List<ProcessorAttribute> attributes = includeExclude.attributes;
            Map<AttributeKey<?>, Pattern> attributeKeyValuePatterns = new HashMap<>();
            if (attributes != null) {
                for (ProcessorAttribute attribute : attributes) {
                    if (attribute.value != null) {
                        attributeKeyValuePatterns.put(AttributeKey.stringKey(attribute.key), Pattern.compile(attribute.value));
                    }
                }
            }

            List<Pattern> spanPatterns = new ArrayList<>();
            if (includeExclude.spanNames != null) {
                for (String regex : includeExclude.spanNames) {
                    spanPatterns.add(Pattern.compile(regex));
                }
            }

            List<Pattern> logPatterns = new ArrayList<>();
            if (includeExclude.logNames != null) {
                for (String regex : includeExclude.logNames) {
                    logPatterns.add(Pattern.compile(regex));
                }
            }

            return new RegexpIncludeExclude(spanPatterns, logPatterns, attributeKeyValuePatterns);
        }

        // Function to compare span attribute value with user provided value
        private static boolean isAttributeValueMatch(String attributeValue, Pattern valuePattern) {
            return valuePattern.matcher(attributeValue).find();
        }

        private static boolean isPatternFound(SpanData span, List<Pattern> patterns) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(span.getName()).find()) {
                    // pattern matches the span!!!
                    return true;
                }
            }
            //no pattern matched
            return false;
        }

        // Function to compare span with user provided span patterns
        public boolean isMatch(SpanData span, boolean isLog) {

            if(spanPatterns.isEmpty() && logPatterns.isEmpty()) {
                // check attributes for both spans and logs
                return checkAttributes(span);
            }

            if(isLog) {
                if(logPatterns.isEmpty()) return false;
                if(!logPatterns.isEmpty() && !isPatternFound(span, logPatterns)) return false;
            } else {
                if(spanPatterns.isEmpty()) return false;
                if(!spanPatterns.isEmpty() && !isPatternFound(span, spanPatterns)) return false;
            }

            return checkAttributes(span);
        }

        // Function to compare span with user provided attributes list
        private boolean checkAttributes(SpanData span) {
            for (Entry<AttributeKey<?>, Pattern> attributeEntry : attributeValuePatterns.entrySet()) {
                //All of these attributes must match exactly for a match to occur.
                Object existingAttributeValue = span.getAttributes().get(attributeEntry.getKey());
                if (!(existingAttributeValue instanceof String)) {
                    // user specified key not found
                    return false;
                }
                if (attributeEntry.getValue() != null && !isAttributeValueMatch((String) existingAttributeValue, attributeEntry.getValue())) {
                    // user specified value doesn't match
                    return false;
                }
            }
            // everything matched!!!
            return true;
        }

    }

}
