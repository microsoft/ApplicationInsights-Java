/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
