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
  public static AttributeProcessor create(ProcessorConfig config) {
    IncludeExclude normalizedInclude =
        config.include != null ? getNormalizedIncludeExclude(config.include) : null;
    IncludeExclude normalizedExclude =
        config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
    return new AttributeProcessor(config.actions, normalizedInclude, normalizedExclude);
  }

  // this won't be needed once we update to 0.13.0
  // see https://github.com/open-telemetry/opentelemetry-java/pull/2284
  public static String getAttribute(Attributes attributes, AttributeKey<String> key) {
    Object existingValueObj = attributes.get(key);
    // checking the return type won't be needed once we update to 0.13.0
    // see https://github.com/open-telemetry/opentelemetry-java/pull/2284
    if (existingValueObj instanceof String) {
      return (String) existingValueObj;
    }
    return null;
  }

  // Function to process actions
  public SpanData processActions(SpanData span) {
    SpanData updatedSpan = span;
    for (ProcessorAction actionObj : actions) {
      updatedSpan = processAction(updatedSpan, actionObj);
    }
    return updatedSpan;
  }

  private static SpanData processAction(SpanData span, ProcessorAction actionObj) {
    switch (actionObj.action) {
      case INSERT:
        return processInsertAction(span, actionObj);
      case UPDATE:
        return processUpdateAction(span, actionObj);
      case DELETE:
        return processDeleteAction(span, actionObj);
      case HASH:
        return procesHashAction(span, actionObj);
      case EXTRACT:
        return processExtractAction(span, actionObj);
    }
    return span;
  }

  private static SpanData processInsertAction(SpanData span, ProcessorAction actionObj) {
    Attributes existingSpanAttributes = span.getAttributes();
    // Update from existing attribute
    if (actionObj.value != null) {
      // update to new value
      AttributesBuilder builder = Attributes.builder();
      builder.put(actionObj.key, actionObj.value);
      builder.putAll(existingSpanAttributes);
      return new MySpanData(span, builder.build());
    }
    String fromAttributeValue = getAttribute(existingSpanAttributes, actionObj.fromAttribute);
    if (fromAttributeValue != null) {
      AttributesBuilder builder = Attributes.builder();
      builder.put(actionObj.key, fromAttributeValue);
      builder.putAll(existingSpanAttributes);
      return new MySpanData(span, builder.build());
    }
    return span;
  }

  private static SpanData processUpdateAction(SpanData span, ProcessorAction actionObj) {
    // Currently we only support String
    String existingValue = getAttribute(span.getAttributes(), actionObj.key);
    if (existingValue == null) {
      return span;
    }
    // Update from existing attribute
    if (actionObj.value != null) {
      // update to new value
      AttributesBuilder builder = span.getAttributes().toBuilder();
      builder.put(actionObj.key, actionObj.value);
      return new MySpanData(span, builder.build());
    }
    String fromAttributeValue = getAttribute(span.getAttributes(), actionObj.fromAttribute);
    if (fromAttributeValue != null) {
      AttributesBuilder builder = span.getAttributes().toBuilder();
      builder.put(actionObj.key, fromAttributeValue);
      return new MySpanData(span, builder.build());
    }
    return span;
  }

  private static SpanData processDeleteAction(SpanData span, ProcessorAction actionObj) {
    // Currently we only support String
    String existingValue = getAttribute(span.getAttributes(), actionObj.key);
    if (existingValue == null) {
      return span;
    }
    AttributesBuilder builder = Attributes.builder();
    span.getAttributes()
        .forEach(
            (key, value) -> {
              if (!key.equals(actionObj.key)) {
                putIntoBuilder(builder, key, value);
              }
            });
    return new MySpanData(span, builder.build());
  }

  private static SpanData procesHashAction(SpanData span, ProcessorAction actionObj) {
    // Currently we only support String
    String existingValue = getAttribute(span.getAttributes(), actionObj.key);
    if (existingValue == null) {
      return span;
    }
    AttributesBuilder builderCopy = span.getAttributes().toBuilder();
    builderCopy.put(actionObj.key, DigestUtils.sha1Hex(existingValue));
    return new MySpanData(span, builderCopy.build());
  }

  private static SpanData processExtractAction(SpanData span, ProcessorAction actionObj) {
    // Currently we only support String
    String existingValue = getAttribute(span.getAttributes(), actionObj.key);
    if (existingValue == null) {
      return span;
    }
    Matcher matcher = actionObj.extractAttribute.pattern.matcher(existingValue);
    if (!matcher.matches()) {
      return span;
    }
    AttributesBuilder builder = span.getAttributes().toBuilder();
    for (String groupName : actionObj.extractAttribute.groupNames) {
      builder.put(groupName, matcher.group(groupName));
    }
    return new MySpanData(span, builder.build());
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
