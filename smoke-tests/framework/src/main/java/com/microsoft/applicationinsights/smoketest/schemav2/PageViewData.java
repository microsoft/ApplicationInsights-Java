// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

/** Data contract class PageViewData. */
public class PageViewData extends EventData {
  /** Backing field for property Url. */
  private String url;

  /** Backing field for property Duration. */
  private Duration duration = new Duration(0);

  /** Initializes a new instance of the PageViewData class. */
  public PageViewData() {}

  /** Gets the Url property. */
  public String getUrl() {
    return this.url;
  }

  /** Sets the Url property. */
  public void setUrl(String value) {
    this.url = value;
  }

  /** Gets the Duration property. */
  public Duration getDuration() {
    return this.duration;
  }

  /** Sets the Duration property. */
  public void setDuration(Duration value) {
    this.duration = value;
  }
}
