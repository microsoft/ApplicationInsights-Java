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

import com.microsoft.applicationinsights.agent.internal.configuration.Patterns;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessorUtil {

  public static String applyRule(
      List<String> groupNamesList, Pattern pattern, String name, AttributesBuilder builder) {
    if (groupNamesList.isEmpty()) {
      return name;
    }
    Matcher matcher = pattern.matcher(name);
    StringBuilder sb = new StringBuilder();
    int lastEnd = 0;
    // As of now we are considering only first match.
    if (matcher.find()) {
      sb.append(name, lastEnd, matcher.start());
      int innerLastEnd = matcher.start();
      for (int i = 1; i <= groupNamesList.size(); i++) {
        sb.append(name, innerLastEnd, matcher.start(i));
        sb.append("{");
        sb.append(groupNamesList.get(i - 1));
        // add attribute key=groupNames.get(i-1), value=matcher.group(i)
        builder.put(groupNamesList.get(i - 1), matcher.group(i));
        sb.append("}");
        innerLastEnd = matcher.end(i);
      }
      sb.append(name, innerLastEnd, matcher.end());
      lastEnd = matcher.end();
    }
    sb.append(name, lastEnd, name.length());

    return sb.toString();
  }

  public static List<List<String>> getGroupNamesList(List<String> toAttributeRules) {
    List<List<String>> groupNamesList = new ArrayList<>();
    for (String rule : toAttributeRules) {
      groupNamesList.add(Patterns.getGroupNames(rule));
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

  private ProcessorUtil() {}
}
