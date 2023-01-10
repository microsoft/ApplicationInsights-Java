// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.regex.Matcher;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;

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
  public static AttributeProcessor create(ProcessorConfig config, boolean isLog) {
    IncludeExclude normalizedInclude =
        config.include != null ? getNormalizedIncludeExclude(config.include, isLog) : null;
    IncludeExclude normalizedExclude =
        config.exclude != null ? getNormalizedIncludeExclude(config.exclude, isLog) : null;
    return new AttributeProcessor(config.actions, normalizedInclude, normalizedExclude);
  }

  // Process actions on SpanData
  public SpanData processActions(SpanData span) {
    SpanData result = span;
    for (ProcessorAction actionObj : actions) {
      result = new MySpanData(result, processAction(result.getAttributes(), actionObj));
    }

    return result;
  }

  // Process actions on LogRecordData
  public LogRecordData processActions(LogRecordData log) {
    LogRecordData result = log;
    for (ProcessorAction actionObj : actions) {
      result = new MyLogData(result, processAction(result.getAttributes(), actionObj));
    }

    return result;
  }

  private static Attributes processAction(Attributes attributes, ProcessorAction actionObj) {
    switch (actionObj.action) {
      case INSERT:
        return processInsertAction(attributes, actionObj);
      case UPDATE:
        return processUpdateAction(attributes, actionObj);
      case DELETE:
        return processDeleteAction(attributes, actionObj);
      case HASH:
        return processHashAction(attributes, actionObj);
      case EXTRACT:
        return processExtractAction(attributes, actionObj);
      case MASK:
        return processMaskAction(attributes, actionObj);
    }
    return attributes;
  }

  private static Attributes processInsertAction(Attributes attributes, ProcessorAction actionObj) {
    Attributes existingAttributes = attributes;

    // Update from existing attribute
    if (actionObj.value != null) {
      // update to new value
      AttributesBuilder builder = Attributes.builder();
      builder.put(actionObj.key, actionObj.value);
      builder.putAll(existingAttributes);
      return builder.build();
    }

    String fromAttributeValue = existingAttributes.get(actionObj.fromAttribute);
    if (fromAttributeValue != null) {
      AttributesBuilder builder = Attributes.builder();
      builder.put(actionObj.key, fromAttributeValue);
      builder.putAll(existingAttributes);
      return builder.build();
    }

    return attributes;
  }

  private static Attributes processUpdateAction(Attributes attributes, ProcessorAction actionObj) {
    Attributes existingAttributes = attributes;

    // Currently we only support String
    String existingValue = existingAttributes.get(actionObj.key);
    if (existingValue == null) {
      return existingAttributes;
    }

    // Update from existing attribute
    if (actionObj.value != null) {
      // update to new value
      AttributesBuilder builder = existingAttributes.toBuilder();
      builder.put(actionObj.key, actionObj.value);
      return builder.build();
    }

    String fromAttributeValue = existingAttributes.get(actionObj.fromAttribute);
    if (fromAttributeValue != null) {
      AttributesBuilder builder = existingAttributes.toBuilder();
      builder.put(actionObj.key, fromAttributeValue);
      return builder.build();
    }

    return attributes;
  }

  private static Attributes processDeleteAction(Attributes attributes, ProcessorAction actionObj) {
    Attributes existingAttributes = attributes;

    // Currently we only support String
    String existingValue = existingAttributes.get(actionObj.key);
    if (existingValue == null) {
      return existingAttributes;
    }

    AttributesBuilder builder = Attributes.builder();
    existingAttributes.forEach(
        (key, value) -> {
          if (!key.equals(actionObj.key)) {
            putIntoBuilder(builder, key, value);
          }
        });
    return builder.build();
  }

  private static Attributes processHashAction(Attributes attributes, ProcessorAction actionObj) {
    Attributes existingAttributes = attributes;

    // Currently we only support String
    String existingValue = existingAttributes.get(actionObj.key);
    if (existingValue == null) {
      return existingAttributes;
    }

    AttributesBuilder builderCopy = existingAttributes.toBuilder();
    builderCopy.put(actionObj.key, DigestUtils.sha256Hex(existingValue));
    return builderCopy.build();
  }

  private static Attributes processExtractAction(Attributes attributes, ProcessorAction actionObj) {
    Attributes existingAttributes = attributes;

    // Currently we only support String
    String existingValue = existingAttributes.get(actionObj.key);
    if (existingValue == null) {
      return existingAttributes;
    }
    Matcher matcher = actionObj.extractAttribute.pattern.matcher(existingValue);
    if (!matcher.matches()) {
      return existingAttributes;
    }

    AttributesBuilder builder = existingAttributes.toBuilder();
    for (String groupName : actionObj.extractAttribute.groupNames) {
      builder.put(groupName, matcher.group(groupName));
    }
    return builder.build();
  }

  private static Attributes processMaskAction(Attributes attributes, ProcessorAction actionObj) {
    Attributes existingAttributes = attributes;

    // Currently we only support String
    String existingValue = existingAttributes.get(actionObj.key);
    if (existingValue == null) {
      return existingAttributes;
    }

    Matcher matcher = actionObj.maskAttribute.pattern.matcher(existingValue);
    String newValue = matcher.replaceAll(actionObj.maskAttribute.replace);
    if (newValue.equals(existingValue)) {
      return existingAttributes;
    }

    AttributesBuilder builder = existingAttributes.toBuilder();
    builder.put(actionObj.key, newValue);
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private static void putIntoBuilder(AttributesBuilder builder, AttributeKey<?> key, Object value) {
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
    }
  }
}
