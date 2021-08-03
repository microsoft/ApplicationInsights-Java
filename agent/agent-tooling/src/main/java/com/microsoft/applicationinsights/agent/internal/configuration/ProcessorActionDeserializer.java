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

package com.microsoft.applicationinsights.agent.internal.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ProcessorActionDeserializer extends StdDeserializer<Configuration.ProcessorAction> {

  protected static final Pattern capturingGroupNames =
      Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

  protected ProcessorActionDeserializer() {
    this(null);
  }

  protected ProcessorActionDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Configuration.ProcessorAction deserialize(
      JsonParser jsonParser, DeserializationContext context) throws IOException {
    // TODO change how we initialize pattern
    String pattern = "empty pattern";
    try {
      Configuration.ProcessorAction processorAction = new Configuration.ProcessorAction();
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
      if (node.get("key") != null) {
        processorAction.key = node.get("key").asText();
      }
      if (node.get("action") != null) {
        processorAction.action = getActionType(node.get("action").asText());
      }
      if (node.get("fromAttribute") != null) {
        processorAction.fromAttribute = node.get("fromAttribute").asText();
      }
      if (node.get("value") != null) {
        processorAction.value = node.get("value").asText();
      }
      if (node.get("pattern") != null) {
        pattern = node.get("pattern").asText();
        Pattern regexPattern = Pattern.compile(pattern);
        List<String> groupNames = getGroupNames(pattern);
        processorAction.extractAttribute =
            new Configuration.ExtractAttribute(regexPattern, groupNames);
      }
      return processorAction;
    } catch (PatternSyntaxException e) {
      throw new FriendlyException(
          "Telemetry processor configuration does not have valid regex:" + pattern,
          "Please provide a valid regex in the telemetry processors configuration. "
              + "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557",
          e);
    }
  }

  private static List<String> getGroupNames(String regex) {
    List<String> groupNames = new ArrayList<>();
    Matcher matcher = capturingGroupNames.matcher(regex);
    while (matcher.find()) {
      groupNames.add(matcher.group(1));
    }
    return groupNames;
  }

  private static Configuration.ProcessorActionType getActionType(String action) {
    if (action == null) {
      return null;
    }
    for (Configuration.ProcessorActionType actionType :
        Configuration.ProcessorActionType.values()) {
      if (actionType.toString().toLowerCase().equals(action.toLowerCase())) {
        return actionType;
      }
    }
    return null;
  }
}
