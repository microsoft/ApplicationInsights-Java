// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

@SuppressWarnings("MemberName")
public enum SeverityLevel {
  Verbose(0),
  Information(1),
  Warning(2),
  Error(3),
  Critical(4);

  private final int id;

  public int getValue() {
    return id;
  }

  SeverityLevel(int id) {
    this.id = id;
  }
}
