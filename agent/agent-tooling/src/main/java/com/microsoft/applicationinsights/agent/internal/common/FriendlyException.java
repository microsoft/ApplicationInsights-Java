// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.common;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import javax.annotation.Nullable;

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
