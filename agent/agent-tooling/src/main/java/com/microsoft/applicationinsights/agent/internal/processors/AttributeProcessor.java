/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.regex.Matcher;
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

  // Process actions on LogData
  public LogData processActions(LogData log) {
    LogData result = log;
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
      return  builder.build();
    }

    String fromAttributeValue = existingAttributes.get(actionObj.fromAttribute);
    if (fromAttributeValue != null) {
      AttributesBuilder builder = Attributes.builder();
      builder.put(actionObj.key, fromAttributeValue);
      builder.putAll(existingAttributes);
      return  builder.build();
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
    existingAttributes
        .forEach(
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
    builderCopy.put(actionObj.key, DigestUtils.sha1Hex(existingValue));
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
