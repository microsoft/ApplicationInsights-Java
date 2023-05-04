// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.StringJoiner;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@SuppressWarnings({"Java8ApiChecker"})
@Name("com.microsoft.applicationinsights.diagnostics.jfr.Telemetry")
@Label("Telemetry")
@Category("Diagnostic")
@Description("Telemetry")
@StackTrace(false)
@Period("100 ms")
public class Telemetry extends Event {
  public static final String NAME = "com.microsoft.applicationinsights.diagnostics.jfr.Telemetry";

  public static final int LATEST_VERSION = 3;

  public final int version;
  public final String telemetry;

  @JsonCreator
  public Telemetry(Integer version, String telemetry) {
    if (version == null) {
      this.version = 1;
    } else {
      this.version = version;
    }
    this.telemetry = telemetry;
  }

  public Telemetry(String telemetry) {
    this.telemetry = telemetry;
    this.version = LATEST_VERSION;
  }

  public Telemetry(List<Double> telemetry) {
    StringJoiner joiner = new StringJoiner(",");
    telemetry.forEach(
        it -> {
          if (it == null) {
            joiner.add("null");
          } else {
            joiner.add(Double.toString(it));
          }
        });
    this.telemetry = joiner.toString();
    this.version = LATEST_VERSION;
  }

  public int getVersion() {
    return version;
  }
}
