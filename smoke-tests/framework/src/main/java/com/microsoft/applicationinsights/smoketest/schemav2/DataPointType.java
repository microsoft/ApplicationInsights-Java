// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import com.google.gson.annotations.SerializedName;

/** Enum DataPointType. */
public enum DataPointType {
  @SerializedName("Measurement")
  MEASUREMENT(0),
  @SerializedName("Aggregation")
  AGGREGATION(1);

  private final int id;

  public int getValue() {
    return id;
  }

  DataPointType(int id) {
    this.id = id;
  }
}
