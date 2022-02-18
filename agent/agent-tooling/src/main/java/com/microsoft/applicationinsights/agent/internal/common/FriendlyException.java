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

import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FriendlyException extends RuntimeException {

  public FriendlyException(String description, String action) {
    this(description, action, null);
  }

  public FriendlyException(String description, String action, @Nullable Throwable cause) {
    super(buildMessage(description, action), cause);
  }

  public String getMessageWithBanner(String banner) {
    return System.lineSeparator()
        + "*************************"
        + System.lineSeparator()
        + banner
        + System.lineSeparator()
        + "*************************"
        + System.lineSeparator()
        + getMessage();
  }

  private static String buildMessage(String description, String action) {
    StringBuilder messageBuilder = new StringBuilder();
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
    return messageBuilder.toString();
  }
}
