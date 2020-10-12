package com.microsoft.applicationinsights.agent.internal.processors;

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
import org.apache.commons.codec.digest.DigestUtils;

// structure which only allows valid data
// normalization has to occur before construction
public class SpanProcessor {
    private final List<SpanProcessorAction> insertActions;
    private final List<SpanProcessorAction> otherActions;
    private final IncludeExclude include;
    private final IncludeExclude exclude;
    private final boolean isValidConfig;

    public SpanProcessor(List<SpanProcessorAction> insertActions, List<SpanProcessorAction> otherActions, IncludeExclude include,
                         IncludeExclude exclude, boolean isValidConfig) {
        this.insertActions = insertActions;
        this.otherActions = otherActions;
        this.include = include;
        this.exclude = exclude;
        this.isValidConfig = isValidConfig;
    }

    private static SpanProcessor getEmptySpanProcessor() {
        IncludeExclude emptyIncludeExclude = new StrictIncludeExclude(new ArrayList<>(), new ArrayList<>());
        return new SpanProcessor(new ArrayList<>(), new ArrayList<>(),
                emptyIncludeExclude, emptyIncludeExclude, false);
    }

    private static SpanProcessor getNormalizedSpanProcessor(SpanProcessorConfig config) {
        IncludeExclude normalizedInclude = null;
        IncludeExclude normalizedExclude = null;
        List<SpanProcessorAction> insertActions = new ArrayList<>();
        List<SpanProcessorAction> otherActions = new ArrayList<>();
        if (config.include != null) {
            normalizedInclude = config.include.matchType == SpanProcessorMatchType.strict ? StrictIncludeExclude.create(config.include) : RegexpIncludeExclude.create(config.include);
        }
        if (config.exclude != null) {
            normalizedExclude = config.exclude.matchType == SpanProcessorMatchType.strict ? StrictIncludeExclude.create(config.exclude) : RegexpIncludeExclude.create(config.exclude);
        }
        for (SpanProcessorAction spanProcessorAction : config.actions) {

            if (spanProcessorAction.action == SpanProcessorActionType.insert) {
                insertActions.add(spanProcessorAction);
            } else {
                otherActions.add(spanProcessorAction);
            }
        }
        return new SpanProcessor(insertActions, otherActions, normalizedInclude, normalizedExclude, true);
    }

    //Copy from existing attribute
    private static boolean copyFromExistingAttribute(Builder insertBuilder, ReadableAttributes existingSpanAttributes, SpanProcessorAction actionObj) {
        AttributeValue existingSpanAttributeValue = existingSpanAttributes.get(actionObj.fromAttribute);
        if (existingSpanAttributeValue != null) {
            insertBuilder.setAttribute(actionObj.key, existingSpanAttributes.get(actionObj.fromAttribute));
            return true;
        }
        return false;
    }

    public static SpanProcessor create(SpanProcessorConfig config) {
        return config.isValid() ? getNormalizedSpanProcessor(config) : getEmptySpanProcessor();
    }

    public IncludeExclude getInclude() {
        return include;
    }

    public IncludeExclude getExclude() {
        return exclude;
    }

    public boolean hasValidConfig() {
        return isValidConfig;
    }

    public SpanData processInsertActions(SpanData span) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Builder insertBuilder = Attributes.newBuilder();
        boolean insertedFlag = false; // Flag to check if insert operation is successful
        for (SpanProcessorAction actionObj : insertActions) {
            if (actionObj.value != null) {
                //Insert new attribute
                insertBuilder.setAttribute(actionObj.key, actionObj.value);
                insertedFlag = true;
            } else if (copyFromExistingAttribute(insertBuilder, existingSpanAttributes, actionObj)) {
                //Copy from existing attribute
                insertedFlag = true;
            }
        }
        if (insertedFlag) {
            // Copy all existing attributes
            existingSpanAttributes.forEach(new KeyValueConsumer<AttributeValue>() {
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
        // loop over existing attributes
        existingSpanAttributes.forEach(new KeyValueConsumer<AttributeValue>() {

            @Override
            public void consume(String existingKey, AttributeValue existingValue) {
                boolean updatedFlag = false;// flag to check if a attribute is updated
                for (SpanProcessorAction actionObj : otherActions) {
                    if (actionObj.key.equals(existingKey)) {
                        switch (actionObj.action) {
                            case update:
                                if (actionObj.value != null) {
                                    //update to new value
                                    builder.setAttribute(actionObj.key, actionObj.value);
                                    updatedFlag = true;
                                } else if (copyFromExistingAttribute(builder, existingSpanAttributes, actionObj)) {
                                    //Update from existing attribute
                                    updatedFlag = true;
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
                                        builder.setAttribute(actionObj.key, DigestUtils.sha1Hex(existingSpanAttributeValue.getStringValue()));
                                        updatedFlag = true;
                                    }
                                }
                                break;
                        }
                    }
                }
                if (!updatedFlag) {
                    builder.setAttribute(existingKey, existingValue);
                }
            }
        });
        // loop through insert actions, if key is not in keys set then call builder.setAttribute()
        return new MySpanData(span, builder.build());
    }


    public static abstract class IncludeExclude {
        //All of these attributes must match exactly for a match to occur
        //Only match_type=strict is allowed if "attributes" are specified.
        protected final List<SpanProcessorAttribute> attributes;

        public IncludeExclude(List<SpanProcessorAttribute> attributes) {
            this.attributes = attributes;
        }

        private static boolean isAttributeValueMatch(AttributeValue attributeValue, String value) {
            return attributeValue.getType() == Type.STRING && attributeValue.getStringValue().equals(value);
        }

        public abstract boolean isMatch(SpanData span);

        public List<SpanProcessorAttribute> getAttributes() {
            return attributes;
        }

        public boolean checkAttributes(SpanData span) {
            for (SpanProcessorAttribute attribute : this.getAttributes()) {
                //All of these attributes must match exactly for a match to occur.
                AttributeValue existingAttributeValue = span.getAttributes().get(attribute.key);
                if (existingAttributeValue == null) {
                    // user specified key not found
                    return false;
                }
                if (attribute.value != null && !isAttributeValueMatch(existingAttributeValue, attribute.value)) {
                    // user specified value doesn't match
                    return false;
                }
            }
            // everything matched!!!
            return true;
        }
    }

    public static class StrictIncludeExclude extends IncludeExclude {

        private final List<String> spanNames;

        public StrictIncludeExclude(List<SpanProcessorAttribute> attributes, List<String> spanNames) {
            super(attributes);
            this.spanNames = spanNames;
        }

        public static StrictIncludeExclude create(SpanProcessorIncludeExclude includeExclude) {
            List<SpanProcessorAttribute> attributes = includeExclude.attributes;
            if (attributes == null) {
                attributes = new ArrayList<>();
            }
            List<String> spanNames = includeExclude.spanNames;
            if (spanNames == null) {
                spanNames = new ArrayList<>();
            }
            return new StrictIncludeExclude(attributes, spanNames);
        }

        public boolean isMatch(SpanData span) {
            if (!spanNames.isEmpty() && !spanNames.contains(span.getName())) {
                // span name doesn't match
                return false;
            }
            return this.checkAttributes(span);
        }
    }

    public static class RegexpIncludeExclude extends IncludeExclude {

        private final List<Pattern> spanPatterns;

        public RegexpIncludeExclude(List<SpanProcessorAttribute> attributes, List<Pattern> spanPatterns) {
            super(attributes);
            this.spanPatterns = spanPatterns;
        }

        public static RegexpIncludeExclude create(SpanProcessorIncludeExclude includeExclude) {
            List<SpanProcessorAttribute> attributes = includeExclude.attributes;
            if (attributes == null) {
                attributes = new ArrayList<>();
            }
            List<Pattern> spanPatterns = new ArrayList<>();
            if (includeExclude.spanNames != null) {
                for (String regex : includeExclude.spanNames) {
                    spanPatterns.add(Pattern.compile(regex));
                }
            }
            return new RegexpIncludeExclude(attributes, spanPatterns);
        }

        public boolean isMatch(SpanData span) {
            if (!spanPatterns.isEmpty() && !isPatternFound(span)) {
                return false;
            }
            return checkAttributes(span);
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
    }


}
