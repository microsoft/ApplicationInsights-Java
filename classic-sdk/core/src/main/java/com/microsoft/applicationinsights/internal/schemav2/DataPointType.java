// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

@SuppressWarnings("MemberName")
public enum DataPointType {
  Measurement(0),
  Aggregation(1);

  private final int id;

  public int getValue() {
    return id;
  }

  DataPointType(int id) {
    this.id = id;
  }
}
