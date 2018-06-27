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

package com.microsoft.applicationinsights.internal.channel.sampling;

import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.junit.Assert;
import org.junit.Test;

/** Created by gupele on 11/2/2016. */
public class FixedRateTelemetrySamplerTest {

  @Test
  public void testWith100Percent() {
    FixedRateTelemetrySampler tested = new FixedRateTelemetrySampler();
    tested.setIncludeTypes("Event, PageView");
    boolean result = tested.isSampledIn(new EventTelemetry());

    Assert.assertTrue(result);
  }

  @Test
  public void testWith0Percent() {
    FixedRateTelemetrySampler tested = new FixedRateTelemetrySampler();
    tested.setIncludeTypes("Event, PageView");
    tested.setSamplingPercentage(0.0);
    boolean result = tested.isSampledIn(new EventTelemetry());

    Assert.assertFalse(result);
  }

  @Test
  public void testWith0PercentWithUserId() {
    FixedRateTelemetrySampler tested = new FixedRateTelemetrySampler();
    tested.setIncludeTypes("Event, PageView");
    tested.setSamplingPercentage(0.0);
    Telemetry telemetry = new EventTelemetry();
    telemetry.getContext().getUser().setId("B");
    boolean result = tested.isSampledIn(telemetry);

    Assert.assertFalse(result);
  }

  @Test
  public void testWith100PercentWithUserId() {
    FixedRateTelemetrySampler tested = new FixedRateTelemetrySampler();
    tested.setExcludeTypes("Event, PageView");
    tested.setSamplingPercentage(100.0);
    Telemetry telemetry = new EventTelemetry();
    telemetry.getContext().getUser().setId("B");
    boolean result = tested.isSampledIn(telemetry);

    Assert.assertTrue(result);
  }

  @Test
  public void testWith90PercentWithOpId() {
    FixedRateTelemetrySampler tested = new FixedRateTelemetrySampler();
    tested.setSamplingPercentage(10.0);
    Telemetry telemetry = new EventTelemetry();
    telemetry.getContext().getOperation().setId("a");
    boolean result = tested.isSampledIn(telemetry);

    Assert.assertFalse(result);
  }

  @Test
  public void testWith99PercentWithOpId() {
    FixedRateTelemetrySampler tested = new FixedRateTelemetrySampler();
    tested.setSamplingPercentage(99.0);
    Telemetry telemetry = new EventTelemetry();
    telemetry.getContext().getOperation().setId("a");
    boolean result = tested.isSampledIn(telemetry);
    telemetry.getContext().getOperation().setId("aa");
    result = tested.isSampledIn(telemetry);
    telemetry.getContext().getOperation().setId("aaasdfsadfasdfsadfsadfsdafasfsadfsdf");
    result = tested.isSampledIn(telemetry);

    Assert.assertTrue(result);
  }

  @Test
  public void testWith0PercentWithOpIdAndExclude() {
    FixedRateTelemetrySampler tested = new FixedRateTelemetrySampler();
    tested.setSamplingPercentage(0.0);
    tested.setExcludeTypes("Event, PageView");
    Telemetry telemetry = new EventTelemetry();
    telemetry.getContext().getOperation().setId("a");
    boolean result = tested.isSampledIn(telemetry);

    Assert.assertTrue(result);
  }
}
