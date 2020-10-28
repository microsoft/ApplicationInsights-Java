package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorConfig;
import io.opentelemetry.common.AttributeConsumer;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Attributes.Builder;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.internal.Utils;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.commons.codec.digest.DigestUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

// structure which only allows valid data
// normalization has to occur before construction
public class AttributeProcessor extends AgentProcessor {
    private final List<ProcessorAction> insertActions;
    private final List<ProcessorAction> otherActions;

    private AttributeProcessor(List<ProcessorAction> insertActions,
                               List<ProcessorAction> otherActions,
                               @Nullable IncludeExclude include,
                               @Nullable IncludeExclude exclude,
                               boolean isValidConfig) {
        super(include, exclude, isValidConfig);
        this.insertActions = insertActions;
        this.otherActions = otherActions;
    }

    // Creates a Span Processor object
    public static AttributeProcessor create(ProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        List<ProcessorAction> insertActions = new ArrayList<>();
        List<ProcessorAction> otherActions = new ArrayList<>();
        for (ProcessorAction ProcessorAction : config.actions) {

            if (ProcessorAction.action == ProcessorActionType.insert) {
                insertActions.add(ProcessorAction);
            } else {
                otherActions.add(ProcessorAction);
            }
        }
        return new AttributeProcessor(insertActions, otherActions, normalizedInclude, normalizedExclude, true);
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

    // Update,delete or calculate Hash values on existing attributes
    public SpanData processOtherActions(SpanData span) {
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        final Attributes.Builder builder = Attributes.newBuilder();
        // loop over existing attributes
        existingSpanAttributes.forEach(
                // TODO optomize this further
                new AttributeConsumer() {
                    @Override
                    public <T> void consume(AttributeKey<T> key, T value) {
                        boolean updatedFlag = false;// flag to check if a attribute is updated
                        for (ProcessorAction actionObj : otherActions) {
                            if (!key.getKey().equals(actionObj.key)) {
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
                                    if (value instanceof String) {
                                        // Currently we only support String
                                        builder.setAttribute(actionObj.key, DigestUtils.sha1Hex(String.valueOf(value)));
                                        updatedFlag = true;
                                    }
                                    break;
                                default:
                                    break; // no action. Added to escape spotbug failures.
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

    private boolean applyUpdateAction(ProcessorAction actionObj, ReadableAttributes existingSpanAttributes, Attributes.Builder builder) {
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
        for (ProcessorAction actionObj : insertActions) {
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

}
