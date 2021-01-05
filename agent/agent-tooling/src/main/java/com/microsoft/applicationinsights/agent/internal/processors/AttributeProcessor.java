package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.List;
import java.util.regex.Matcher;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.commons.codec.digest.DigestUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

// structure which only allows valid data
// normalization has to occur before construction
public class AttributeProcessor extends AgentProcessor {

    private final List<ProcessorAction> actions;

    private AttributeProcessor(
            List<ProcessorAction> actions,
            @Nullable IncludeExclude include,
            @Nullable IncludeExclude exclude) {
        super(include, exclude);
        this.actions = actions;
    }

    // Creates a Span Processor object
    public static AttributeProcessor create(ProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        return new AttributeProcessor(config.actions, normalizedInclude, normalizedExclude);
    }

    // Copy from existing attribute.
    // Returns true if attribute has been found and copied. Else returns false.
    // ToDo store fromAttribute as AttributeKey<?> to avoid creating AttributeKey each time
    private static boolean copyFromExistingAttribute(AttributesBuilder insertBuilder, Attributes existingSpanAttributes, ProcessorAction actionObj) {
        Object existingSpanAttributeValue = existingSpanAttributes.get(AttributeKey.stringKey(actionObj.fromAttribute));
        if (existingSpanAttributeValue instanceof String) {
            insertBuilder.put(actionObj.key, (String) existingSpanAttributeValue);
            return true;
        }
        return false;
    }

    // Function to process actions
    public SpanData processActions(SpanData span) {
        SpanData updatedSpan = span;
        for (ProcessorAction actionObj : actions) {
            updatedSpan = actionObj.action == ProcessorActionType.insert ? processInsertAction(updatedSpan, actionObj) : processOtherAction(updatedSpan, actionObj);
        }
        return updatedSpan;
    }

    private SpanData processOtherAction(SpanData span, ProcessorAction actionObj) {
        final Attributes existingSpanAttributes = span.getAttributes();
        AttributesBuilder builderCopy= span.getAttributes().toBuilder();
        final boolean[] spanUpdateFlag = new boolean[1];
        Object existingValue = existingSpanAttributes.get(AttributeKey.stringKey(actionObj.key));
        switch (actionObj.action) {
            case update:
                // TODO don't instantiate new AttributeKey every time
                if (existingValue != null && applyUpdateAction(actionObj, existingSpanAttributes, builderCopy)) {
                    spanUpdateFlag[0] = true;
                }
                break;
            case delete:
                final AttributesBuilder builder = Attributes.builder();
                existingSpanAttributes.forEach((key, value) -> {
                    if (key.getKey().equals(actionObj.key)) {
                        spanUpdateFlag[0] = true;
                    } else {
                        putIntoBuilder(builder, key, value);
                    }
                });
                return spanUpdateFlag[0] ? new MySpanData(span, builder.build()) : span;
            case hash:
                if (existingValue != null && (existingValue instanceof String)) {
                    // Currently we only support String
                    builderCopy.put(actionObj.key, DigestUtils.sha1Hex((String) existingValue));
                    spanUpdateFlag[0] = true;
                }
                break;
            case extract:
                if (existingValue != null && (existingValue instanceof String)) {
                    Matcher matcher = actionObj.extractAttribute.extractAttributePattern.matcher((String) existingValue);
                    if(matcher.matches()) {
                        spanUpdateFlag[0] = true;
                        for(String groupName: actionObj.extractAttribute.extractAttributeGroupNames) {
                            builderCopy.put(groupName, matcher.group(groupName));
                        }
                    }
                }
                break;
            default:
                break; // no action. Added to escape spotBug failures.
        }

        return spanUpdateFlag[0] ? new MySpanData(span, builderCopy.build()) : span;
    }

    @SuppressWarnings("unchecked")
    private void putIntoBuilder(AttributesBuilder builder, AttributeKey<?> key, Object value) {
        switch (key.getType()) {
            case STRING:
                builder.put((AttributeKey<String>) key, (String) value);
                break;
            case LONG:
                builder.put((AttributeKey<Long>) key, (Long) value);
                break;
            case BOOLEAN:
                builder.put((AttributeKey<Boolean>) key, (Boolean) value);
                break;
            case DOUBLE:
                builder.put((AttributeKey<Double>) key, (Double) value);
                break;
            case STRING_ARRAY:
            case LONG_ARRAY:
            case BOOLEAN_ARRAY:
            case DOUBLE_ARRAY:
                builder.put((AttributeKey<List<?>>) key, (List<?>) value);
                break;
            default:
                // TODO log at least a debug level message
                break;
        }
    }


    private SpanData processInsertAction(SpanData span, ProcessorAction actionObj) {
        Attributes existingSpanAttributes = span.getAttributes();
        final AttributesBuilder insertBuilder = Attributes.builder();
        if (applyUpdateAction(actionObj, existingSpanAttributes, insertBuilder)) {
            // Copy all existing attributes
            insertBuilder.putAll(existingSpanAttributes);
            return new MySpanData(span, insertBuilder.build());
        }
        return span;
    }

    private boolean applyUpdateAction(ProcessorAction actionObj, Attributes existingSpanAttributes, AttributesBuilder builder) {
        //Update from existing attribute
        if (actionObj.value != null) {
            //update to new value
            builder.put(actionObj.key, actionObj.value);
            return true;
        } else {
            return copyFromExistingAttribute(builder, existingSpanAttributes, actionObj);
        }
    }
}
