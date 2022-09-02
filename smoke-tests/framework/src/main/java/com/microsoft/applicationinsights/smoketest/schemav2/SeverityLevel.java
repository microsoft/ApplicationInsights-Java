// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import com.google.gson.annotations.SerializedName;

/** Enum SeverityLevel. */
public enum SeverityLevel {
  @SerializedName("Verbose")
  VERBOSE(0),
  @SerializedName("Information")
  INFORMATION(1),
  @SerializedName("Warning")
  WARNING(2),
  @SerializedName("Error")
  ERROR(3),
  @SerializedName("Critical")
  CRITICAL(4);

  private final int id;

  public int getValue() {
    return id;
  }

  SeverityLevel(int id) {
    this.id = id;
  }
}
