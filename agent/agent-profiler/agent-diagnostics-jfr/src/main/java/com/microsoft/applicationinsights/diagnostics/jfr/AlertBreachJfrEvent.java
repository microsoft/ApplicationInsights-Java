// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@SuppressWarnings({"Java8ApiChecker"})
@Name("com.microsoft.applicationinsights.diagnostics.jfr.AlertBreach")
@Label("AlertBreach")
@Category("Diagnostic")
@Description("AlertBreach")
@StackTrace(false)
public class AlertBreachJfrEvent extends Event {

  public static final String NAME = "com.microsoft.applicationinsights.diagnostics.jfr.AlertBreach";

  private final String alertBreach;

  public AlertBreachJfrEvent(String alertBreach) {
    this.alertBreach = alertBreach;
  }

  public String getAlertBreach() {
    return alertBreach;
  }
}
