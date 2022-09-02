// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.httpclient;

public class NoSupportedProtocolsException extends RuntimeException {
  public NoSupportedProtocolsException(String message) {
    super(message);
  }
}
