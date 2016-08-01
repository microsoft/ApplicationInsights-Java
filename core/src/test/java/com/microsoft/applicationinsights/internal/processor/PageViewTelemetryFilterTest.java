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

import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

/**
 * Created by gupele on 7/26/2016.
 */
public class PageViewTelemetryFilterTest {

    @Test
    public void testProcessNotPageViewTelemetry() throws Exception {
        PageViewTelemetryFilter tested = new PageViewTelemetryFilter();
        boolean result = tested.process(new MetricTelemetry());
        Assert.assertTrue(result);
    }

    @Test
    public void testProcessWithNull() throws Throwable {
        PageViewTelemetryFilter tested = new PageViewTelemetryFilter();
        boolean result = tested.process(null);
        Assert.assertTrue(result);
    }

    @Test
    public void testSetNotNeededNames() throws Throwable {
        PageViewTelemetryFilter tested = new PageViewTelemetryFilter();
        tested.setNotNeededNames("1, 2   , 3,4");

        PageViewTelemetry pvt = new PageViewTelemetry();
        pvt.setUrl(new URI("http://www.microsoft.com/"));

        for (int i = 1; i < 5; ++i) {
            pvt.setName(String.valueOf(i));
            boolean result = tested.process(pvt);
            Assert.assertFalse(result);
        }
    }

    @Test
    public void testSetNotNeededUrls() throws Throwable {
        PageViewTelemetryFilter tested = new PageViewTelemetryFilter();
        tested.setNotNeededUrls("url1, url2/2, url3,url4");
        PageViewTelemetry pvt = new PageViewTelemetry();

        pvt.setUrl(new URI("http://wwww.url1.com/asdf"));
        boolean result = tested.process(pvt);
        Assert.assertFalse(result);

        pvt.setUrl(new URI("http://www.aaa.com/asdf/url2/2/a"));
        result = tested.process(pvt);
        Assert.assertFalse(result);

        pvt.setUrl(new URI("http://www.aaa.com/asdf/url32/2/a"));
        result = tested.process(pvt);
        Assert.assertFalse(result);

        pvt.setUrl(new URI("http://www.aaa.com/asdf/url4/2/a"));
        result = tested.process(pvt);
        Assert.assertFalse(result);

        pvt.setUrl(new URI("http://www.aaa.com/asdf/url5/2/a"));
        result = tested.process(pvt);
        Assert.assertTrue(result);
    }

    @Test
    public void testSetDuration() throws Exception {
        PageViewTelemetryFilter tested = new PageViewTelemetryFilter();
        tested.setDurationThresholdInMS("1000");
        PageViewTelemetry pvt = new PageViewTelemetry();
        pvt.setUrl(new URI("http://www.microsoft.com/"));

        pvt.setDuration(1000);
        boolean result = tested.process(pvt);
        Assert.assertTrue(result);

        pvt.setDuration(1001);
        result = tested.process(pvt);
        Assert.assertTrue(result);

        pvt.setDuration(999);
        result = tested.process(pvt);
        Assert.assertFalse(result);
    }

    @Test
    public void testSetBadDuration() throws Exception {
        PageViewTelemetryFilter tested = new PageViewTelemetryFilter();
        tested.setDurationThresholdInMS("1000");
        PageViewTelemetry pvt = new PageViewTelemetry();
        pvt.setUrl(new URI("http://www.microsoft.com/"));
        pvt.setDuration(1000);

        boolean result = tested.process(pvt);
        Assert.assertTrue(result);

        pvt.setDuration(1001);
        result = tested.process(pvt);
        Assert.assertTrue(result);

        pvt.setDuration(999);
        result = tested.process(pvt);
        Assert.assertFalse(result);
    }
}
