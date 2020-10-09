package com.microsoft.applicationinsights.agent.internal.processors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorMatchType;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.common.AttributeValue.Type;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Attributes.Builder;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.common.ReadableKeyValuePairs.KeyValueConsumer;
import io.opentelemetry.sdk.trace.data.SpanData;

// structure which only allows valid data
// normalization has to occur before construction
public class SpanProcessor {
    private final List<SpanProcessorAction> insertActions = new ArrayList<>();
    private final List<SpanProcessorAction> otherActions = new ArrayList<>();
    private IncludeExclude include;
    private IncludeExclude exclude;
    private boolean isValidConfig;

    private static SpanProcessor getEmptySpanProcessor() {
        SpanProcessor emptyObj = new SpanProcessor();
        emptyObj.isValidConfig = false;
        emptyObj.include = StrictIncludeExclude.getEmptyIncludeExclude();
        emptyObj.exclude = StrictIncludeExclude.getEmptyIncludeExclude();
        return emptyObj;
    }

    private static SpanProcessor normalize(SpanProcessorConfig config) {
        SpanProcessor normalizedSpanProcessor = new SpanProcessor();
        normalizedSpanProcessor.isValidConfig = true;

        if (config.include != null) {
            normalizedSpanProcessor.include = config.include.matchType == SpanProcessorMatchType.strict ? StrictIncludeExclude.create(config.include) : RegexpIncludeExclude.create(config.include);
        }

        if (config.exclude != null) {
            normalizedSpanProcessor.exclude = config.exclude.matchType == SpanProcessorMatchType.strict ? StrictIncludeExclude.create(config.exclude) : RegexpIncludeExclude.create(config.exclude);
        }

        for (SpanProcessorAction spanProcessorAction : config.actions) {

            if (spanProcessorAction.action == SpanProcessorActionType.insert) {
                normalizedSpanProcessor.insertActions.add(spanProcessorAction);
            } else {
                normalizedSpanProcessor.otherActions.add(spanProcessorAction);
            }
        }

        return normalizedSpanProcessor;

    }

    public static SpanProcessor create(SpanProcessorConfig config) {
        return config.isValid() ? normalize(config) : getEmptySpanProcessor();
    }

    public IncludeExclude getInclude() {
        return include;
    }

    public IncludeExclude getExclude() {
        return exclude;
    }

    public List<SpanProcessorAction> getInsertActions() {
        return insertActions;
    }

    public List<SpanProcessorAction> getOtherActions() {
        return otherActions;
    }

    public boolean isValidConfig() {
        return isValidConfig;
    }

    public boolean hasValidConfig() {
        return this.isValidConfig;
    }

    public SpanData processInsertActions(SpanData span) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Builder insertBuilder = Attributes.newBuilder();
        boolean insertedFlag = false;
        for (SpanProcessorAction actionObj : this.getInsertActions()) {
            if (actionObj.value != null) {
                insertBuilder.setAttribute(actionObj.key, actionObj.value);
                insertedFlag = true;
            } else {
                if (existingSpanAttributes.get(actionObj.fromAttribute) != null) {
                    insertBuilder.setAttribute(actionObj.key, existingSpanAttributes.get(actionObj.fromAttribute));
                    insertedFlag = true;
                }
                insertBuilder.setAttribute(actionObj.key, actionObj.value);
            }
        }
        if (insertedFlag) {
            span.getAttributes().forEach(new KeyValueConsumer<AttributeValue>() {
                public void consume(String key, AttributeValue value) {
                    insertBuilder.setAttribute(key, value);
                }
            });
            return new MySpanData(span, insertBuilder.build());
        }
        return span;
    }

    public SpanData processOtherActions(SpanData span) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Builder builder = Attributes.newBuilder();
        List<SpanProcessorAction> _otherActions = this.getOtherActions();
        // loop over existing attributes
        span.getAttributes().forEach(new KeyValueConsumer<AttributeValue>() {

            @Override
            public void consume(String key, AttributeValue value) {
                boolean updatedFlag = false;// flag to check if a attribute is updated
                for (SpanProcessorAction actionObj : _otherActions) {
                    if (actionObj.key.equals(key)) {
                        switch (actionObj.action) {
                            case update:
                                if (actionObj.value != null) {
                                    builder.setAttribute(actionObj.key, actionObj.value);
                                    updatedFlag = true;
                                } else if (actionObj.fromAttribute != null) {
                                    AttributeValue existingSpanAttributeValue = existingSpanAttributes.get(actionObj.fromAttribute);
                                    if (existingSpanAttributeValue != null) {
                                        builder.setAttribute(actionObj.key, existingSpanAttributeValue);
                                        updatedFlag = true;
                                    }
                                }
                                break;
                            case delete:
                                // Return without copying the existing span attribute
                                return;
                            case hash:
                                AttributeValue existingSpanAttributeValue = existingSpanAttributes.get(actionObj.key);
                                if (existingSpanAttributeValue != null) {
                                    // Currently we only support String
                                    if (existingSpanAttributeValue.getType() == Type.STRING) {
                                        builder.setAttribute(actionObj.key, getSHA1(existingSpanAttributeValue.getStringValue()));
                                        updatedFlag = true;
                                    }
                                }
                                break;
                        }
                    }
                }
                if (!updatedFlag) {
                    builder.setAttribute(key, value);
                }
            }
        });
        // loop through insert actions, if key is not in keys set then call builder.setAttribute()
        return new MySpanData(span, builder.build());
    }

    private String getSHA1(String value) {
        MessageDigest mDigest = null;
        try {
            mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(value.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return value;
        }
    }

    public static abstract class IncludeExclude {
        //All of these attributes must match exactly for a match to occur
        //Only match_type=strict is allowed if "attributes" are specified.
        protected final List<SpanProcessorAttribute> attributes = new ArrayList<>();

        protected SpanProcessorMatchType matchType;

        public abstract boolean isMatch(SpanData span);

        public List<SpanProcessorAttribute> getAttributes() {
            return attributes;
        }

        public SpanProcessorMatchType getMatchType() {
            return matchType;
        }

        public void setMatchType(SpanProcessorMatchType matchType) {
            this.matchType = matchType;
        }

        public boolean checkAttributes(SpanData span) {
            for (SpanProcessorAttribute attribute : this.getAttributes()) {
                //All of these attributes must match exactly for a match to occur.
                AttributeValue existingAttributeValue = span.getAttributes().get(attribute.key);
                if (existingAttributeValue != null) {
                    //found a match
                    if (attribute.value != null) {
                        if (existingAttributeValue.getType() != Type.STRING || !existingAttributeValue.getStringValue().equals(attribute.value)) {
                            return false;
                            // value mismatch found.
                        }
                    }
                } else {
                    return false;
                }
            }
            return true;
        }
    }

    public static class StrictIncludeExclude extends IncludeExclude {

        private final List<String> spanNames = new ArrayList<>();

        public static StrictIncludeExclude getEmptyIncludeExclude() {
            StrictIncludeExclude emptyObj = new StrictIncludeExclude();
            emptyObj.setMatchType(SpanProcessorMatchType.strict);
            return emptyObj;
        }

        public static StrictIncludeExclude create(SpanProcessorIncludeExclude includeExclude) {
            StrictIncludeExclude strictObj = new StrictIncludeExclude();
            if (includeExclude.attributes != null) {
                strictObj.getAttributes().addAll(includeExclude.attributes);
            }
            if (includeExclude.spanNames != null) {
                strictObj.getSpanNames().addAll(includeExclude.spanNames);
            }
            strictObj.setMatchType(includeExclude.matchType);
            return strictObj;
        }

        public List<String> getSpanNames() {
            return spanNames;
        }

        public boolean isMatch(SpanData span) {
            if (!this.getSpanNames().isEmpty()) {
                if (!this.getSpanNames().contains(span.getName())) {
                    //match found
                    return false;
                }
            }
            return this.checkAttributes(span);
        }
    }

    public static class RegexpIncludeExclude extends IncludeExclude {

        private final List<Pattern> spanPatterns = new ArrayList<>();

        public static RegexpIncludeExclude getEmptyIncludeExclude() {
            RegexpIncludeExclude emptyObj = new RegexpIncludeExclude();
            emptyObj.setMatchType(SpanProcessorMatchType.regexp);
            return emptyObj;
        }

        public static RegexpIncludeExclude create(SpanProcessorIncludeExclude includeExclude) {
            RegexpIncludeExclude regexObj = new RegexpIncludeExclude();
            if (includeExclude.attributes != null) {
                regexObj.getAttributes().addAll(includeExclude.attributes);
            }
            if (includeExclude.spanNames != null) {
                for (String regex : includeExclude.spanNames) {
                    regexObj.getSpanPatterns().add(Pattern.compile(regex));
                }
            }
            regexObj.setMatchType(includeExclude.matchType);
            return regexObj;
        }

        public List<Pattern> getSpanPatterns() {
            return spanPatterns;
        }

        public boolean isMatch(SpanData span) {
            boolean matchFound = false;
            if (!this.getSpanPatterns().isEmpty()) {
                for (Pattern pattern : this.getSpanPatterns()) {
                    if (pattern.matcher(span.getName()).find()) {
                        matchFound = true;
                        break;
                    }
                }
            } else {
                matchFound = true;
            }
            if (!matchFound) return false;
            return checkAttributes(span);
        }
    }


}
