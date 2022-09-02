// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Data contract class Envelope. */
@SuppressWarnings("unused")
public class Envelope {
  /** Backing field for property Name. */
  private String name;

  /** Backing field for property Time. */
  private String time;

  /** Backing field for property SampleRate. */
  private Float sampleRate;

  /** Backing field for property IKey. */
  @SuppressWarnings("checkstyle:MemberName")
  private String iKey;

  /** Backing field for property Tags. */
  private ConcurrentMap<String, String> tags;

  /** Backing field for property Data. */
  private Base data;

  /** Initializes a new instance of the Envelope class. */
  public Envelope() {}

  /** Sets the Name property. */
  public void setName(String value) {
    this.name = value;
  }

  /** Sets the Time property. */
  public void setTime(String value) {
    this.time = value;
  }

  public String getTime() {
    return time;
  }

  /** Sets the SampleRate property. */
  public void setSampleRate(Float value) {
    this.sampleRate = value;
  }

  public Float getSampleRate() {
    return sampleRate;
  }

  /** Gets the IKey property. */
  // used by smoke tests
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public String getIKey() {
    return this.iKey;
  }

  /** Sets the IKey property. */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public void setIKey(String value) {
    this.iKey = value;
  }

  /** Gets the Tags property. */
  public ConcurrentMap<String, String> getTags() {
    if (this.tags == null) {
      this.tags = new ConcurrentHashMap<>();
    }
    return this.tags;
  }

  /** Sets the Tags property. */
  public void setTags(ConcurrentMap<String, String> value) {
    this.tags = value;
  }

  /** Gets the Data property. */
  public Base getData() {
    return this.data;
  }

  /** Sets the Data property. */
  public void setData(Base value) {
    this.data = value;
  }
}
