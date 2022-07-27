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

package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tracestate {

  private static final String KEY_WITHOUT_VENDOR_FORMAT = "[a-z][_0-9a-z\\-\\*\\/]{0,255}";
  private static final String KEY_WITH_VENDOR_FORMAT =
      "[a-z][_0-9a-z\\-\\*\\/]{0,240}@[a-z][_0-9a-z\\-\\*\\/]{0,13}";
  private static final String KEY_FORMAT = KEY_WITHOUT_VENDOR_FORMAT + "|" + KEY_WITH_VENDOR_FORMAT;
  private static final String VALUE_FORMAT =
      "[\\x20-\\x2b\\x2d-\\x3c\\x3e-\\x7e]{0,255}[\\x21-\\x2b\\x2d-\\x3c\\x3e-\\x7e]";

  private static final String DELIMITER_FORMAT = "[ \\t]*,[ \\t]*";
  private static final String MEMBER_FORMAT =
      String.format("(%s)(=)(%s)", KEY_FORMAT, VALUE_FORMAT);

  private static final Pattern DELIMITER_FORMAT_RE = Pattern.compile(DELIMITER_FORMAT);
  private static final Pattern MEMBER_FORMAT_RE = Pattern.compile("^" + MEMBER_FORMAT + "$");

  private static final int MAX_KEY_VALUE_PAIRS = 32;

  private final LinkedHashMap<String, String> internalList =
      new LinkedHashMap<>(MAX_KEY_VALUE_PAIRS);

  private final String internalString;

  public Tracestate(String input) {
    if (input == null) {
      throw new IllegalArgumentException("input is null");
    }

    String[] values = DELIMITER_FORMAT_RE.split(input);
    for (String item : values) {
      Matcher m = MEMBER_FORMAT_RE.matcher(item);
      if (!m.find()) {
        throw new IllegalArgumentException(String.format("invalid string %s in tracestate", item));
      }
      String key = m.group(1);
      String value = m.group(3);
      if (internalList.get(key) != null) {
        throw new IllegalArgumentException(String.format("duplicated keys %s in tracestate", key));
      }
      internalList.put(key, value);
    }
    if (internalList.size() > MAX_KEY_VALUE_PAIRS) {
      throw new IllegalArgumentException(
          String.format("cannot have more than %d key-value pairs", MAX_KEY_VALUE_PAIRS));
    }
    internalString = toInternalString();
  }

  public String get(String key) {
    return internalList.get(key);
  }

  @Override
  public String toString() {
    return internalString;
  }

  private String toInternalString() {
    boolean isFirst = true;
    StringBuilder stringBuilder = new StringBuilder(512);
    for (Map.Entry<String, String> entry : internalList.entrySet()) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(",");
      }
      stringBuilder.append(entry.getKey());
      stringBuilder.append("=");
      stringBuilder.append(entry.getValue());
    }
    return stringBuilder.toString();
  }
}
