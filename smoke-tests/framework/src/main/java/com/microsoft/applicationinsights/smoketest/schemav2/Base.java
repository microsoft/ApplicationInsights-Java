// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

/** Data contract class Base. */
public class Base {
  /** Backing field for property BaseType. */
  private String baseType;

  /** Initializes a new instance of the Base class. */
  public Base() {}

  /** Gets the BaseType property. */
  public String getBaseType() {
    return this.baseType;
  }

  /** Sets the BaseType property. */
  public void setBaseType(String value) {
    this.baseType = value;
  }
}
