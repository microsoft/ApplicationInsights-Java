// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

/** Data contract class Data. */
public class Data<T extends Domain> extends Base {
  /** Backing field for property BaseData. */
  private T baseData;

  /** Initializes a new instance of the Data{TDomain} class. */
  public Data() {}

  /** Gets the BaseData property. */
  public T getBaseData() {
    return this.baseData;
  }

  /** Sets the BaseData property. */
  public void setBaseData(T value) {
    this.baseData = value;
  }
}
