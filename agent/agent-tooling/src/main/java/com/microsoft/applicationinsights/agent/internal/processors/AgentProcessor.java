package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorMatchType;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AgentProcessor {
    private final @Nullable IncludeExclude include;
    private final @Nullable IncludeExclude exclude;
    private final boolean isValidConfig;

    public AgentProcessor(@Nullable IncludeExclude include,
                          @Nullable IncludeExclude exclude,
                          boolean isValidConfig) {
        this.include = include;
        this.exclude = exclude;
        this.isValidConfig = isValidConfig;
    }

    protected static AttributeProcessor.IncludeExclude getNormalizedIncludeExclude(ProcessorIncludeExclude includeExclude) {
        return includeExclude.matchType == ProcessorMatchType.strict ? AttributeProcessor.StrictIncludeExclude.create(includeExclude) : AttributeProcessor.RegexpIncludeExclude.create(includeExclude);
    }

    public @Nullable IncludeExclude getInclude() {
        return include;
    }

    public @Nullable IncludeExclude getExclude() {
        return exclude;
    }

    public boolean hasValidConfig() {
        return isValidConfig;
    }

    public static abstract class IncludeExclude {
        //All of these attributes must match exactly for a match to occur
        protected final List<ProcessorAttribute> attributes;

        public IncludeExclude(List<ProcessorAttribute> attributes) {
            this.attributes = attributes;
        }

        // Function to compare span with user provided span names or span patterns
        public abstract boolean isMatch(SpanData span);

    }

    public static class StrictIncludeExclude extends IncludeExclude {

        private final List<String> spanNames;

        public StrictIncludeExclude(List<ProcessorAttribute> attributes, List<String> spanNames) {
            super(attributes);
            this.spanNames = spanNames;
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
            return new StrictIncludeExclude(attributes, spanNames);
        }

        // Function to compare span with user provided span names
        public boolean isMatch(SpanData span) {
            if (!spanNames.isEmpty() && !spanNames.contains(span.getName())) {
                // span name doesn't match
                return false;
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
                if (attribute.value != null && !String.valueOf(existingAttributeValue).equals(attribute.value)) {
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
        private final HashMap<String, Pattern> attributeValuePatterns;

        public RegexpIncludeExclude(List<ProcessorAttribute> attributes, List<Pattern> spanPatterns, HashMap<String, Pattern> attributeValuePatterns) {
            super(attributes);
            this.spanPatterns = spanPatterns;
            this.attributeValuePatterns = attributeValuePatterns;
        }

        public static RegexpIncludeExclude create(ProcessorIncludeExclude includeExclude) {
            List<ProcessorAttribute> attributes = includeExclude.attributes;
            HashMap<String, Pattern> attributeKeyValuePatterns = new HashMap<>();
            if (attributes == null) {
                attributes = new ArrayList<>();
            } else {
                for (ProcessorAttribute attribute : attributes) {
                    attributeKeyValuePatterns.put(attribute.key, Pattern.compile(attribute.value));
                }
            }
            List<Pattern> spanPatterns = new ArrayList<>();
            if (includeExclude.spanNames != null) {
                for (String regex : includeExclude.spanNames) {
                    spanPatterns.add(Pattern.compile(regex));
                }
            }

            return new RegexpIncludeExclude(attributes, spanPatterns, attributeKeyValuePatterns);
        }

        // Function to compare span attribute value with user provided value
        private static boolean isAttributeValueMatch(String attributeValue, Pattern valuePattern) {
            return valuePattern.matcher(attributeValue).find();
        }

        private boolean isPatternFound(SpanData span) {
            for (Pattern pattern : spanPatterns) {
                if (pattern.matcher(span.getName()).find()) {
                    // pattern matches the span!!!
                    return true;
                }
            }
            //no pattern matched
            return false;
        }

        // Function to compare span with user provided span patterns
        public boolean isMatch(SpanData span) {
            if (!spanPatterns.isEmpty() && !isPatternFound(span)) {
                return false;
            }
            return checkAttributes(span);
        }

        // Function to compare span with user provided attributes list
        private boolean checkAttributes(SpanData span) {
            for (ProcessorAttribute attribute : attributes) {
                //All of these attributes must match exactly for a match to occur.
                Object existingAttributeValue = span.getAttributes().get(AttributeKey.stringKey(attribute.key));
                if (!(existingAttributeValue instanceof String)) {
                    // user specified key not found
                    return false;
                }
                if (attribute.value != null && !isAttributeValueMatch(String.valueOf(existingAttributeValue), attributeValuePatterns.get(attribute.key))) {
                    // user specified value doesn't match
                    return false;
                }
            }
            // everything matched!!!
            return true;
        }

    }

}
