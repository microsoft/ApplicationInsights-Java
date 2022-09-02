// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorIncludeExclude;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public abstract class AgentProcessor {

  @Nullable private final IncludeExclude include;
  @Nullable private final IncludeExclude exclude;

  protected AgentProcessor(@Nullable IncludeExclude include, @Nullable IncludeExclude exclude) {
    this.include = include;
    this.exclude = exclude;
  }

  protected static IncludeExclude getNormalizedIncludeExclude(
      ProcessorIncludeExclude includeExclude, boolean isLog) {
    return includeExclude.matchType == MatchType.STRICT
        ? AgentProcessor.StrictIncludeExclude.create(includeExclude, isLog)
        : AgentProcessor.RegexpIncludeExclude.create(includeExclude, isLog);
  }

  @Nullable
  public IncludeExclude getInclude() {
    return include;
  }

  @Nullable
  public IncludeExclude getExclude() {
    return exclude;
  }

  public abstract static class IncludeExclude {
    // Function to compare span/log with user provided span/log names or span/log patterns
    public abstract boolean isMatch(Attributes attributes, String name);
  }

  public static class StrictIncludeExclude extends IncludeExclude {
    private final List<ProcessorAttribute> processorAttributes;
    private final List<String> names;

    private StrictIncludeExclude(List<ProcessorAttribute> processorAttributes, List<String> names) {
      this.processorAttributes = processorAttributes;
      this.names = names;
    }

    public static StrictIncludeExclude create(
        ProcessorIncludeExclude includeExclude, boolean isLog) {
      List<ProcessorAttribute> attributes = includeExclude.attributes;
      if (attributes == null) {
        attributes = new ArrayList<>();
      }

      List<String> names = isLog ? includeExclude.logBodies : includeExclude.spanNames;
      if (names == null) {
        names = new ArrayList<>();
      }
      return new StrictIncludeExclude(attributes, names);
    }

    // compare span/log with user provided span/log names
    @Override
    public boolean isMatch(Attributes attributes, String name) {
      if (!names.isEmpty() && !names.contains(name)) {
        return false;
      }

      return this.checkAttributes(attributes);
    }

    // Function to compare span with user provided attributes list
    private boolean checkAttributes(Attributes attributes) {
      for (ProcessorAttribute attribute : processorAttributes) {
        // All of these attributes must match exactly for a match to occur.
        Object existingAttributeValue = attributes.get(AttributeKey.stringKey(attribute.key));
        // to get the string value
        // existingAttributeValue.toString()
        // String.valueOf(existingAttributeValue);
        if (!(existingAttributeValue instanceof String)) {
          // user specified key not found
          return false;
        }
        if (attribute.value != null && !existingAttributeValue.equals(attribute.value)) {
          // user specified value doesn't match
          return false;
        }
      }
      // everything matched!!!
      return true;
    }
  }

  public static class RegexpIncludeExclude extends IncludeExclude {

    // pattern to match against span names or log bodies
    private final List<Pattern> patterns;
    private final Map<AttributeKey<?>, Pattern> attributeValuePatterns;

    private RegexpIncludeExclude(
        List<Pattern> patterns, Map<AttributeKey<?>, Pattern> attributeValuePatterns) {
      this.patterns = patterns;
      this.attributeValuePatterns = attributeValuePatterns;
    }

    public static RegexpIncludeExclude create(
        ProcessorIncludeExclude includeExclude, boolean isLog) {
      List<ProcessorAttribute> attributes = includeExclude.attributes;
      Map<AttributeKey<?>, Pattern> attributeKeyValuePatterns = new HashMap<>();
      if (attributes != null) {
        for (ProcessorAttribute attribute : attributes) {
          if (attribute.value != null) {
            attributeKeyValuePatterns.put(
                AttributeKey.stringKey(attribute.key), Pattern.compile(attribute.value));
          }
        }
      }

      List<Pattern> patterns = new ArrayList<>();
      if (isLog) {
        if (includeExclude.logBodies != null) {
          for (String regex : includeExclude.logBodies) {
            patterns.add(Pattern.compile(regex));
          }
        }
      } else {
        if (includeExclude.spanNames != null) {
          for (String regex : includeExclude.spanNames) {
            patterns.add(Pattern.compile(regex));
          }
        }
      }

      return new RegexpIncludeExclude(patterns, attributeKeyValuePatterns);
    }

    // Function to compare span attribute value with user provided value
    private static boolean isAttributeValueMatch(String attributeValue, Pattern valuePattern) {
      return valuePattern.matcher(attributeValue).find();
    }

    private static boolean isPatternFound(String name, List<Pattern> patterns) {
      for (Pattern pattern : patterns) {
        if (pattern.matcher(name).find()) {
          // pattern matches the span!!!
          return true;
        }
      }
      // no pattern matched
      return false;
    }

    // Function to compare span/log with user provided span patterns/log patterns
    @Override
    public boolean isMatch(Attributes attributes, String name) {
      if (!patterns.isEmpty() && !isPatternFound(name, patterns)) {
        return false;
      }

      return checkAttributes(attributes);
    }

    // Function to compare span with user provided attributes list
    private boolean checkAttributes(Attributes attributes) {
      for (Entry<AttributeKey<?>, Pattern> attributeEntry : attributeValuePatterns.entrySet()) {
        // All of these attributes must match exactly for a match to occur.
        Object existingAttributeValue = attributes.get(attributeEntry.getKey());
        if (!(existingAttributeValue instanceof String)) {
          // user specified key not found
          return false;
        }
        if (attributeEntry.getValue() != null
            && !isAttributeValueMatch((String) existingAttributeValue, attributeEntry.getValue())) {
          // user specified value doesn't match
          return false;
        }
      }
      // everything matched!!!
      return true;
    }
  }
}
