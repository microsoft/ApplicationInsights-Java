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
// Reserve messageId 2000 - 2099 for java agent
public enum Mdc {
  INITIALIZATION_SUCCESS("messageId", "2000"),
  FREE_PHYSICAL_MEMORY_SIZE_ERROR("messageId", "2001"),
  JMX_METRIC_PERFORMANCE_COUNTER_ERROR("messageId", "2002"),
  FAIL_TO_SEND_STATSBEAT_ERROR("messageId", "2003"),
  CONFIGURATION_RELATED_ERROR("messageId", "2004"),
  OSHI_RELATED_ERROR("messageId", "2005"),
  STATUS_FILE_RELATED_ERROR("messageId", "2006"),
  APP_ID_ERROR("messageId", "2007");

  private final String key;
  private final String value;

  Mdc(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public MdcScope makeActive() {
    return new MdcScope(MDC.putCloseable(key, value));
  }
}
