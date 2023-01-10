// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class NettyVersions {

  // Example: **19** **Group:** `io.netty` **Name:** `netty-codec-dns` **Version:** `4.1.79.Final`
  private static final Pattern DEPENDENCY_COORDINATE_PATTERN =
      Pattern.compile(".*`(.*)`.*`(.*)`.*`(.*)`");

  private NettyVersions() {}

  static String extract() {
    String moreLicenseResource = "/META-INF/licenses/more-licenses.md";
    InputStream moreLicenseAsStream = NettyVersions.class.getResourceAsStream(moreLicenseResource);
    if (moreLicenseAsStream == null) {
      return moreLicenseResource + " notFound";
    }
    return extractNettyVersions(moreLicenseAsStream);
  }

  private static String extractNettyVersions(InputStream moreLicenseAsStream) {
    try (InputStreamReader inputStreamReader =
            new InputStreamReader(moreLicenseAsStream, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
      return bufferedReader
          .lines()
          .filter(line -> line.contains("netty") && line.contains("Group"))
          .map(line -> extractDependency(line.trim()))
          .collect(Collectors.joining(", "));
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  private static String extractDependency(String line) {
    Matcher matcher = DEPENDENCY_COORDINATE_PATTERN.matcher(line);
    boolean matched = matcher.find();
    if (!matched) {
      return "(" + line + ")";
    }
    try {
      return "(" + matcher.group(1) + ", " + matcher.group(2) + ", " + matcher.group(3) + ")";
    } catch (IllegalStateException | IndexOutOfBoundsException e) {
      return "(" + e.getMessage() + " for " + line + ")";
    }
  }
}
