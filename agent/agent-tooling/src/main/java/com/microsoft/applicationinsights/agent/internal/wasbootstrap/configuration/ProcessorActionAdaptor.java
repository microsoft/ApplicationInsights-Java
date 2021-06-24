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

package com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ExtractAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorActionJson;
import com.microsoft.applicationinsights.agent.internal.wascore.common.FriendlyException;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ProcessorActionAdaptor {
  protected static final Pattern capturingGroupNames =
      Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

  public static List<String> getGroupNames(String regex) {
    List<String> groupNames = new ArrayList<>();
    Matcher matcher = capturingGroupNames.matcher(regex);
    while (matcher.find()) {
      groupNames.add(matcher.group(1));
    }
    return groupNames;
  }

  @FromJson
  ProcessorAction fromJson(ProcessorActionJson processorActionJson) {
    try {
      ProcessorAction processorAction = new ProcessorAction();
      processorAction.key = processorActionJson.key;
      processorAction.action = processorActionJson.action;
      processorAction.fromAttribute = processorActionJson.fromAttribute;
      processorAction.value = processorActionJson.value;
      if (processorActionJson.pattern == null) {
        return processorAction; // If pattern not present, no further processing required
      }
      String pattern = processorActionJson.pattern;
      Pattern regexPattern = Pattern.compile(pattern);
      List<String> groupNames = getGroupNames(pattern);
      processorAction.extractAttribute = new ExtractAttribute(regexPattern, groupNames);
      return processorAction;
    } catch (PatternSyntaxException e) {
      throw new FriendlyException(
          "Telemetry processor configuration does not have valid regex:"
              + processorActionJson.pattern,
          "Please provide a valid regex in the telemetry processors configuration. "
              + "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557",
          e);
    }
  }

  @ToJson
  ProcessorActionJson toJson(ProcessorAction processorAction) {
    throw new UnsupportedOperationException();
  }
}
