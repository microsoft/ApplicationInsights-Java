// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patterns {

  private static final Pattern CAPTURING_GROUP_NAMES =
      Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

  public static List<String> getGroupNames(String regex) {
    List<String> groupNames = new ArrayList<>();
    Matcher matcher = CAPTURING_GROUP_NAMES.matcher(regex);
    while (matcher.find()) {
      groupNames.add(matcher.group(1));
    }
    return groupNames;
  }

  private Patterns() {}
}
