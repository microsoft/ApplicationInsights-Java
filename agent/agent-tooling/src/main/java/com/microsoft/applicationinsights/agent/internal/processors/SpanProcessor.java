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
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.commons.codec.digest.DigestUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

// structure which only allows valid data
// normalization has to occur before construction
public class SpanProcessor {
    private final List<SpanProcessorAction> insertActions;
    private final List<SpanProcessorAction> otherActions;
    private final @Nullable IncludeExclude include;
    private final @Nullable IncludeExclude exclude;
    private final boolean isValidConfig;

    private SpanProcessor(List<SpanProcessorAction> insertActions, List<SpanProcessorAction> otherActions, @Nullable IncludeExclude include,
                          @Nullable IncludeExclude exclude, boolean isValidConfig) {
        this.insertActions = insertActions;
        this.otherActions = otherActions;
        this.include = include;
        this.exclude = exclude;
        this.isValidConfig = isValidConfig;
    }

    // Creates a Span Processor object
    public static SpanProcessor create(SpanProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        List<SpanProcessorAction> insertActions = new ArrayList<>();
        List<SpanProcessorAction> otherActions = new ArrayList<>();
        for (SpanProcessorAction spanProcessorAction : config.actions) {

            if (spanProcessorAction.action == SpanProcessorActionType.insert) {
                insertActions.add(spanProcessorAction);
            } else {
                otherActions.add(spanProcessorAction);
            }
        }
        return new SpanProcessor(insertActions, otherActions, normalizedInclude, normalizedExclude, true);
    }

    // Copy from existing attribute.
    // Returns true if attribute has been found and copied. Else returns false.
    private static boolean copyFromExistingAttribute(Attributes.Builder insertBuilder, ReadableAttributes existingSpanAttributes, SpanProcessorAction actionObj) {
        AttributeValue existingSpanAttributeValue = existingSpanAttributes.get(actionObj.fromAttribute);
        if (existingSpanAttributeValue != null) {
            insertBuilder.setAttribute(actionObj.key, existingSpanAttributes.get(actionObj.fromAttribute));
            return true;
        }
        return false;
    }

    private static SpanProcessor.IncludeExclude getNormalizedIncludeExclude(SpanProcessorIncludeExclude includeExclude) {
        return includeExclude.matchType == SpanProcessorMatchType.strict ? StrictIncludeExclude.create(includeExclude) : RegexpIncludeExclude.create(includeExclude);
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

    // Update,delete or calculate Hash values on existing attributes
    public SpanData processOtherActions(SpanData span) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Attributes.Builder builder = Attributes.newBuilder();
        // loop over existing attributes
        existingSpanAttributes.forEach((existingKey, existingValue) -> {
            boolean updatedFlag = false;// flag to check if a attribute is updated
            for (SpanProcessorAction actionObj : otherActions) {
                if (!actionObj.key.equals(existingKey)) {
                    continue;
                }
                switch (actionObj.action) {
                    case update:
                        if (applyUpdateAction(actionObj, existingSpanAttributes, builder)) {
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
            if (!updatedFlag) {
                builder.setAttribute(existingKey, existingValue);
            }
        });
        // loop through insert actions, if key is not in keys set then call builder.setAttribute()
        return new MySpanData(span, builder.build());
    }

    private boolean applyUpdateAction(SpanProcessorAction actionObj, ReadableAttributes existingSpanAttributes, Attributes.Builder builder) {
        //Update from existing attribute
        if (actionObj.value != null) {
            //update to new value
            builder.setAttribute(actionObj.key, actionObj.value);
            return true;
        } else return copyFromExistingAttribute(builder, existingSpanAttributes, actionObj);
    }

    // Insert new Attributes
    public SpanData processInsertActions(SpanData span) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Builder insertBuilder = Attributes.newBuilder();
        boolean insertedFlag = false; // Flag to check if insert operation is successful
        for (SpanProcessorAction actionObj : insertActions) {
            if (applyUpdateAction(actionObj, existingSpanAttributes, insertBuilder)) {
                insertedFlag = true;
            }
        }
        if (insertedFlag) {
            // Copy all existing attributes
            existingSpanAttributes.forEach(insertBuilder::setAttribute);
            return new MySpanData(span, insertBuilder.build());
        }
        return span;
    }

    public static abstract class IncludeExclude {
        //All of these attributes must match exactly for a match to occur
        //Only match_type=strict is allowed if "attributes" are specified.
        protected final List<SpanProcessorAttribute> attributes;

        public IncludeExclude(List<SpanProcessorAttribute> attributes) {
            this.attributes = attributes;
        }

        // Function to compare span attribute value with user provided value
        private static boolean isAttributeValueMatch(AttributeValue attributeValue, String value) {
            return attributeValue.getType() == Type.STRING && attributeValue.getStringValue().equals(value);
        }

        // Function to compare span with user provided span names or span patterns
        public abstract boolean isMatch(SpanData span);

        // Function to compare span with user provided attributes list
        public boolean checkAttributes(SpanData span) {
            for (SpanProcessorAttribute attribute : attributes) {
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

        // Function to compare span with user provided span names
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

        // Function to compare span with user provided span patterns
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
