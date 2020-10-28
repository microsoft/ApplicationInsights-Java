package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.common.AttributeConsumer;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Attributes.Builder;
import io.opentelemetry.common.ReadableAttributes;
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
                               @Nullable IncludeExclude exclude,
                               boolean isValidConfig) {
        super(include, exclude, isValidConfig);
        this.actions = actions;
    }

    // Creates a Span Processor object
    public static AttributeProcessor create(ProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        List<ProcessorAction> actions = new ArrayList<>(config.actions);
        return new AttributeProcessor(actions ,normalizedInclude, normalizedExclude, true);
    }

    // Copy from existing attribute.
    // Returns true if attribute has been found and copied. Else returns false.
    private static boolean copyFromExistingAttribute(Attributes.Builder insertBuilder, ReadableAttributes existingSpanAttributes, ProcessorAction actionObj) {
        Object existingSpanAttributeValue = existingSpanAttributes.get(AttributeKey.stringKey(actionObj.fromAttribute));
        if (existingSpanAttributeValue instanceof String) {
            insertBuilder.setAttribute(actionObj.key, String.valueOf(existingSpanAttributeValue));
            return true;
        }
        return false;
    }

    // Function to process actions
    public SpanData processActions(SpanData span) {
        SpanData prevSpan = span;
        SpanData updatedSpan;
        for(ProcessorAction actionObj: actions) {
            updatedSpan = actionObj.action == ProcessorActionType.insert ? processInsertActions(prevSpan, actionObj) : processOtherActions(prevSpan, actionObj);
            prevSpan = updatedSpan;
        }
        return prevSpan;
    }

    private SpanData processOtherActions(SpanData span, ProcessorAction actionObj) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Attributes.Builder builder = Attributes.newBuilder();
        final boolean[] spanUpdateFlag = new boolean[1]; // This is for optimization. If none of the attributes are updated, we can skip the attributes.build step
        existingSpanAttributes.forEach(// TODO optomize this further
                new AttributeConsumer() {
                    @Override
                    public <T> void consume(AttributeKey<T> key, T value) {
                        boolean attributeUpdatedFlag = false;// flag to check if a attribute is updated
                            if (key.getKey().equals(actionObj.key)) {
                                switch (actionObj.action) {
                                    case update:
                                        if (applyUpdateAction(actionObj, existingSpanAttributes, builder)) {
                                            attributeUpdatedFlag = true;
                                        }
                                        break;
                                    case delete:
                                        // Return without copying the existing span attribute, but still update spanUpdateFlag
                                        spanUpdateFlag[0] = true;
                                        return;
                                    case hash:
                                        if (value instanceof String) {
                                            // Currently we only support String
                                            builder.setAttribute(actionObj.key, DigestUtils.sha1Hex(String.valueOf(value)));
                                            attributeUpdatedFlag = true;
                                        }
                                        break;
                                    default:
                                        break; // no action. Added to escape spotbug failures.
                                }
                            }
                        if (!attributeUpdatedFlag) {
                            builder.setAttribute(key, value);
                        } else {
                            spanUpdateFlag[0] = true;
                        }
                    }
                });
        return spanUpdateFlag[0] ?  new MySpanData(span, builder.build()) : span;
    }

    private SpanData processInsertActions(SpanData span, ProcessorAction actionObj) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Builder insertBuilder = Attributes.newBuilder();
        if (applyUpdateAction(actionObj, existingSpanAttributes, insertBuilder)) {
            // Copy all existing attributes
            existingSpanAttributes.forEach(insertBuilder::setAttribute);
            return new MySpanData(span, insertBuilder.build());
        }
        return span;
    }


    private boolean applyUpdateAction(ProcessorAction actionObj, ReadableAttributes existingSpanAttributes, Attributes.Builder builder) {
        //Update from existing attribute
        if (actionObj.value != null) {
            //update to new value
            builder.setAttribute(actionObj.key, actionObj.value);
            return true;
        } else return copyFromExistingAttribute(builder, existingSpanAttributes, actionObj);
    }


}
