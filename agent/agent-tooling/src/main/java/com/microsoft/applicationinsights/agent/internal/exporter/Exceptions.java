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

package com.microsoft.applicationinsights.agent.internal.exporter;

import static java.util.Collections.singletonList;

import com.microsoft.applicationinsights.agent.internal.exporter.models.builders.ExceptionDetailBuilder;
import java.util.ArrayList;
import java.util.List;

public class Exceptions {

  public static List<ExceptionDetailBuilder> minimalParse(String str) {
    ExceptionDetailBuilder builder = new ExceptionDetailBuilder();
    int separator = -1;
    int length = str.length();
    int current;
    for (current = 0; current < length; current++) {
      char c = str.charAt(current);
      if (c == ':') {
        separator = current;
      } else if (c == '\r' || c == '\n') {
        break;
      }
    }
    // at the end of the loop, current will be end of the first line
    if (separator != -1) {
      String typeName = str.substring(0, separator);
      String message = str.substring(separator + 1, current).trim();
      if (message.isEmpty()) {
        message = typeName;
      }
      builder.setTypeName(typeName);
      builder.setMessage(message);
    } else {
      String typeName = str.substring(0, current);
      builder.setTypeName(typeName);
      builder.setMessage(typeName);
    }
    builder.setStack(str);
    return singletonList(builder);
  }

  // THIS IS UNFINISHED WORK
  // NOT SURE IF IT'S NEEDED
  // TESTING WITH minimalParse() first
  public static List<ExceptionDetailBuilder> fullParse(String str) {
    Parser parser = new Parser();
    for (String line : str.split("\r?\n")) {
      parser.process(line);
    }
    return parser.getDetailBuilders();
  }

  private Exceptions() {}

  private static class Parser {

    private ExceptionDetailBuilder current;
    private final List<ExceptionDetailBuilder> list = new ArrayList<>();

    void process(String line) {
      if (line.charAt(0) != '\t') {
        if (current != null) {
          list.add(current);
        }
        if (line.startsWith("Caused by: ")) {
          line = line.substring("Caused by: ".length());
        }
        current = new ExceptionDetailBuilder();
        int index = line.indexOf(':');
        if (index != -1) {
          String typeName = line.substring(0, index);
          String message = line.substring(index + 1).trim();
          if (message.isEmpty()) {
            message = typeName;
          }
          current.setTypeName(typeName);
          current.setMessage(message);
        } else {
          current.setTypeName(line);
          current.setMessage(line);
        }
      }
    }

    private List<ExceptionDetailBuilder> getDetailBuilders() {
      if (current != null) {
        list.add(current);
      }
      return list;
    }
  }
}
