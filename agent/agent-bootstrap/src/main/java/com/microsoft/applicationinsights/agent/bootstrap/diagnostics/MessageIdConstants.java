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

// JAVA reserves message id for App Service Diagnostics Logs from 2000 - 2999
// Reserve messageId 2000 - 2099 for java agent
public final class MessageIdConstants {

  public static final int ETW_INITIALIZATION_SUCCESS = 2000;
  public static final int FREE_PHYSICAL_MEMORY_SIZE_ERROR = 2001;
  public static final int JMX_METRIC_PERFORMANCE_COUNTER_ERROR = 2002;
  public static final int FAIL_TO_SEND_STATSBEAT_ERROR = 2003;
  public static final int CONFIGURATION_RELATED_ERROR = 2004;
  public static final int OSHI_RELATED_ERROR = 2005;
  public static final int STATUS_FILE_RELATED_ERROR = 2006;
  public static final int APP_ID_ERROR = 2007;

  private MessageIdConstants() {}
}
