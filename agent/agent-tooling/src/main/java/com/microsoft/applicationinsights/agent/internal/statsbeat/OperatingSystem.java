// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.statsbeat;

enum OperatingSystem {
  OS_WINDOWS("Windows"),
  OS_LINUX("Linux"),
  // TODO (heya) should we add Mac/OSX?
  OS_UNKNOWN("unknown");

  private final String value;

  OperatingSystem(String value) {
    this.value = value;
  }

  String getValue() {
    return value;
  }
}
