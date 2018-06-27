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

package com.microsoft.applicationinsights.internal.processor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.junit.Test;

/** Created by gupele on 7/26/2016. */
public class RequestTelemetryFilterTest {

  @Test
  public void testNullTelemetry() throws Throwable {
    RequestTelemetryFilter tested = new RequestTelemetryFilter();
    tested.setNotNeededResponseCodes("200");

    boolean result = tested.process(null);

    assertTrue(result);
  }

  @Test
  public void testNoRequestTelemetry() throws Throwable {
    RequestTelemetryFilter tested = new RequestTelemetryFilter();
    tested.setNotNeededResponseCodes("200");

    boolean result = tested.process(new MetricTelemetry());

    assertTrue(result);
  }

  @Test
  public void testDuration() throws Throwable {
    RequestTelemetryFilter tested = new RequestTelemetryFilter();
    tested.setMinimumDurationInMS("101");

    RequestTelemetry rt = new RequestTelemetry();
    rt.setDuration(new Duration(102));
    boolean result = tested.process(rt);

    assertTrue(result);

    rt.setDuration(new Duration(101));
    result = tested.process(rt);

    assertTrue(result);

    rt.setDuration(new Duration(100));
    result = tested.process(rt);

    assertFalse(result);
  }

  @Test
  public void testErrorCodes() throws Throwable {
    RequestTelemetryFilter tested = new RequestTelemetryFilter();
    tested.setNotNeededResponseCodes("200-400, 500");

    RequestTelemetry rt = new RequestTelemetry();
    for (int i = 200; i <= 600; ++i) {
      rt.setResponseCode(String.valueOf(i));
      boolean result = tested.process(rt);

      if ((i >= 200 && i <= 400) || (i == 500)) {
        assertFalse(result);
      } else {
        assertTrue(result);
      }
    }
  }
}
