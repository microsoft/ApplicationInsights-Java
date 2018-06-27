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

package com.microsoft.applicationinsights.web.utils;

import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.junit.Assert;

/**
 * Created by yonisha on 2/16/2015.
 *
 * <p>This class validates the ThreadContext class. The idea is simple: store a request telemetry
 * context using TLS and then getting it back to validate the expected value. This class should be
 * called concurrently to increase the probability of other threads (validators) interrupting each
 * other, and therefore identify potential bugs. Concurrent invocations of this calls basically
 * simulates concurrent http requests on a web server.
 */
public class ThreadContextValidator extends Thread {
  private long startTicks;

  public ThreadContextValidator() {
    startTicks = DateTimeUtils.getDateTimeNow().getTime();
  }

  @Override
  public void run() {
    RequestTelemetryContext context = new RequestTelemetryContext(startTicks);
    ThreadContext.setRequestTelemetryContext(context);

    validate();
  }

  private void validate() {
    long currentTicks = ThreadContext.getRequestTelemetryContext().getRequestStartTimeTicks();

    Assert.assertEquals("Found a thread with unexpected value.", startTicks, currentTicks);
  }
}
