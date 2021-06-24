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

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.ProcessorActionAdaptor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessorUtil {
  private static final AttributeKey<Boolean> AI_LOG_KEY =
      AttributeKey.booleanKey("applicationinsights.internal.log");

  public static boolean isSpanOfTypeLog(SpanData span) {
    Boolean isLog = span.getAttributes().get(AI_LOG_KEY);
    return isLog != null && isLog;
  }

  public static String applyRule(
      List<String> groupNamesList, Pattern pattern, String spanName, AttributesBuilder builder) {
    if (groupNamesList.isEmpty()) {
      return spanName;
    }
    Matcher matcher = pattern.matcher(spanName);
    StringBuilder sb = new StringBuilder();
    int lastEnd = 0;
    // As of now we are considering only first match.
    if (matcher.find()) {
      sb.append(spanName, lastEnd, matcher.start());
      int innerLastEnd = matcher.start();
      for (int i = 1; i <= groupNamesList.size(); i++) {
        sb.append(spanName, innerLastEnd, matcher.start(i));
        sb.append("{");
        sb.append(groupNamesList.get(i - 1));
        // add attribute key=groupNames.get(i-1), value=matcher.group(i)
        builder.put(groupNamesList.get(i - 1), matcher.group(i));
        sb.append("}");
        innerLastEnd = matcher.end(i);
      }
      sb.append(spanName, innerLastEnd, matcher.end());
      lastEnd = matcher.end();
    }
    sb.append(spanName, lastEnd, spanName.length());

    return sb.toString();
  }

  public static List<List<String>> getGroupNamesList(List<String> toAttributeRules) {
    List<List<String>> groupNamesList = new ArrayList<>();
    for (String rule : toAttributeRules) {
      groupNamesList.add(ProcessorActionAdaptor.getGroupNames(rule));
    }
    return groupNamesList;
  }

  public static boolean spanHasAllFromAttributeKeys(
      SpanData span, List<AttributeKey<?>> fromAttributes) {
    if (fromAttributes.isEmpty()) {
      return false;
    }
    Attributes existingSpanAttributes = span.getAttributes();
    for (AttributeKey<?> attributeKey : fromAttributes) {
      if (existingSpanAttributes.get(attributeKey) == null) {
        return false;
      }
    }
    return true;
  }

  // fromAttributes represents the attribute keys to pull the values from to generate the new span
  // name.
  public static SpanData processFromAttributes(
      SpanData span, List<AttributeKey<?>> fromAttributes, String separator) {
    if (spanHasAllFromAttributeKeys(span, fromAttributes)) {
      StringBuilder updatedSpanBuffer = new StringBuilder();
      Attributes existingSpanAttributes = span.getAttributes();
      for (AttributeKey<?> attributeKey : fromAttributes) {
        updatedSpanBuffer.append(existingSpanAttributes.get(attributeKey));
        updatedSpanBuffer.append(separator);
      }
      // Removing the last appended separator
      if (separator.length() > 0) {
        updatedSpanBuffer.setLength(updatedSpanBuffer.length() - separator.length());
      }
      return new MySpanData(span, span.getAttributes(), updatedSpanBuffer.toString());
    }
    return span;
  }

  // The following function extracts attributes from span name and replaces extracted parts with
  // attribute names
  public static SpanData processToAttributes(
      SpanData span, List<Pattern> toAttributeRulePatterns, List<List<String>> groupNames) {
    if (toAttributeRulePatterns.isEmpty()) {
      return span;
    }

    String spanName = span.getName();
    // copy existing attributes.
    // According to Collector docs, The matched portion
    // in the span name is replaced by extracted attribute name. If the attributes exist
    // they will be overwritten. Need a way to optimize this.
    AttributesBuilder builder = span.getAttributes().toBuilder();
    for (int i = 0; i < groupNames.size(); i++) {
      spanName = applyRule(groupNames.get(i), toAttributeRulePatterns.get(i), spanName, builder);
    }
    return new MySpanData(span, builder.build(), spanName);
  }

  private ProcessorUtil() {}
}
