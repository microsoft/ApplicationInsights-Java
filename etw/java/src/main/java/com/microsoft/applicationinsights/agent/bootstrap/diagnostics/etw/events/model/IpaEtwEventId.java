// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model;

public enum IpaEtwEventId {
  CRITICAL(1),
  ERROR(2),
  WARN(3),
  INFO(4),
  VERBOSE(5);

  private final int idValue;

  private IpaEtwEventId(int idValue) {
    this.idValue = idValue;
  }

  public int value() {
    return idValue;
  }
}
