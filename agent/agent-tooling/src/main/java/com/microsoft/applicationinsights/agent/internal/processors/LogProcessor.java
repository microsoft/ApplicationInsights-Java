// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import static com.microsoft.applicationinsights.agent.internal.processors.ProcessorUtil.applyRule;
import static com.microsoft.applicationinsights.agent.internal.processors.ProcessorUtil.getGroupNamesList;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class LogProcessor extends AgentProcessor {
  private final List<AttributeKey<?>> fromAttributes;
  private final List<Pattern> toAttributeRulePatterns;
  private final List<List<String>> groupNames;
  private final String separator;

  public LogProcessor(
      @Nullable IncludeExclude include,
      @Nullable IncludeExclude exclude,
      List<AttributeKey<?>> fromAttributes,
      List<Pattern> toAttributeRulePatterns,
      List<List<String>> groupNames,
      String separator) {
    super(include, exclude);
    this.fromAttributes = fromAttributes;
    this.toAttributeRulePatterns = toAttributeRulePatterns;
    this.groupNames = groupNames;
    this.separator = separator;
  }

  public static LogProcessor create(ProcessorConfig config) {
    IncludeExclude normalizedInclude =
        config.include != null ? getNormalizedIncludeExclude(config.include, true) : null;
    IncludeExclude normalizedExclude =
        config.exclude != null ? getNormalizedIncludeExclude(config.exclude, true) : null;
    List<AttributeKey<?>> fromAttributes = new ArrayList<>();
    if (config.body.fromAttributes != null) {
      for (String attribute : config.body.fromAttributes) {
        fromAttributes.add(AttributeKey.stringKey(attribute));
      }
    }
    List<String> toAttributeRules = new ArrayList<>();
    if (config.body.toAttributes != null) {
      toAttributeRules.addAll(config.body.toAttributes.rules);
    }
    List<Pattern> toAttributeRulePatterns = new ArrayList<>();
    if (config.body.toAttributes != null) {
      for (String rule : config.body.toAttributes.rules) {
        toAttributeRulePatterns.add(Pattern.compile(rule));
      }
    }
    List<List<String>> groupNames = getGroupNamesList(toAttributeRules);
    String separator = config.body.separator != null ? config.body.separator : "";
    return new LogProcessor(
        normalizedInclude,
        normalizedExclude,
        fromAttributes,
        toAttributeRulePatterns,
        groupNames,
        separator);
  }

  // fromAttributes represents the attribute keys to pull the values from to generate the new log
  // body.
  public LogRecordData processFromAttributes(LogRecordData log) {
    if (logHasAllFromAttributeKeys(log, fromAttributes)) {
      StringBuilder updatedLogBuffer = new StringBuilder();
      Attributes existingLogAttributes = log.getAttributes();
      for (AttributeKey<?> attributeKey : fromAttributes) {
        updatedLogBuffer.append(existingLogAttributes.get(attributeKey));
        updatedLogBuffer.append(separator);
      }
      // Removing the last appended separator
      if (separator.length() > 0) {
        updatedLogBuffer.setLength(updatedLogBuffer.length() - separator.length());
      }

      return new MyLogData(log, existingLogAttributes, Body.string(updatedLogBuffer.toString()));
    }

    return log;
  }

  // The following function extracts attributes from log name and replaces extracted parts with
  // attribute names
  public LogRecordData processToAttributes(LogRecordData log) {
    if (toAttributeRulePatterns.isEmpty()) {
      return log;
    }
    String bodyAsString = log.getBody().asString();
    // copy existing attributes.
    // According to Collector docs, The matched portion
    // in the log name is replaced by extracted attribute name. If the attributes exist
    // they will be overwritten. Need a way to optimize this.
    AttributesBuilder builder = log.getAttributes().toBuilder();
    for (int i = 0; i < groupNames.size(); i++) {
      bodyAsString =
          applyRule(groupNames.get(i), toAttributeRulePatterns.get(i), bodyAsString, builder);
    }

    return new MyLogData(log, builder.build(), Body.string(bodyAsString));
  }

  public static boolean logHasAllFromAttributeKeys(
      LogRecordData log, List<AttributeKey<?>> fromAttributes) {
    if (fromAttributes.isEmpty()) {
      return false;
    }
    Attributes existingSpanAttributes = log.getAttributes();
    for (AttributeKey<?> attributeKey : fromAttributes) {
      if (existingSpanAttributes.get(attributeKey) == null) {
        return false;
      }
    }
    return true;
  }
}
