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

package com.microsoft.applicationinsights.agent.internal.common;

public class FriendlyException extends RuntimeException {
  // TODO (trask) this seems like default for failing to send telemetry, but not necessarily default
  //  for all friendly exceptions
  private static final String DEFAULT_BANNER =
      "ApplicationInsights Java Agent failed to send telemetry data";

  public FriendlyException() {
    super();
  }

  public FriendlyException(String message, String action) {
    // TODO (trask) can these constructors cascade?
    super(populateFriendlyMessage(message, action, DEFAULT_BANNER, ""));
  }

  public FriendlyException(String banner, String action, String message, String note) {
    super(populateFriendlyMessage(message, action, banner, note));
  }

  public FriendlyException(String banner, String action, Throwable cause) {
    super(populateFriendlyMessage("", action, banner, ""), cause);
  }

  public String getMessageWithBanner(String banner) {
    return populateFriendlyMessage(getMessage(), "", banner, "");
  }

  public static String getMessageWithDefaultBanner(String message) {
    return populateFriendlyMessage(message, "", DEFAULT_BANNER, "");
  }

  public static String populateFriendlyMessage(
      String description, String action, String banner, String note) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(System.lineSeparator());
    messageBuilder.append("*************************").append(System.lineSeparator());
    messageBuilder.append(banner).append(System.lineSeparator());
    messageBuilder.append("*************************").append(System.lineSeparator());
    if (!Strings.isNullOrEmpty(description)) {
      messageBuilder.append(System.lineSeparator());
      messageBuilder.append("Description:").append(System.lineSeparator());
      messageBuilder.append(description).append(System.lineSeparator());
    }
    if (!Strings.isNullOrEmpty(action)) {
      messageBuilder.append(System.lineSeparator());
      messageBuilder.append("Action:").append(System.lineSeparator());
      messageBuilder.append(action).append(System.lineSeparator());
    }
    if (!Strings.isNullOrEmpty(note)) {
      messageBuilder.append(System.lineSeparator());
      messageBuilder.append("Note:").append(System.lineSeparator());
      messageBuilder.append(note).append(System.lineSeparator());
    }
    return messageBuilder.toString();
  }
}
