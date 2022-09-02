// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import org.slf4j.MDC;

// JAVA reserves message id for App Service Diagnostics Logs from 2000 - 2999
// Reserve msgId 2000 - 2099 for java agent
public enum MsgId {
  INITIALIZATION_SUCCESS("2000"),
  FREE_MEMORY_METRIC_ERROR("2001"),
  CUSTOM_JMX_METRIC_ERROR("2002"),
  FAIL_TO_SEND_STATSBEAT_ERROR("2003"),
  STATUS_FILE_ERROR("2004"),
  STARTUP_FAILURE_ERROR("2005");

  private final String value;

  MsgId(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public MDC.MDCCloseable makeActive() {
    return MDC.putCloseable(DiagnosticsHelper.MDC_MESSAGE_ID, value);
  }
}
