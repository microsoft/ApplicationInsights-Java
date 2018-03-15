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

package com.microsoft.applicationinsights.internal.channel.inprocess;

import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class InProcessTelemetryChannelTest {

    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";
    private final static String INSTANT_RETRY_NAME = "MaxInstantRetry";
	private final static int DEFAULT_MAX_INSTANT_RETRY = 3;

    @Test(expected = IllegalArgumentException.class)
    public void testNotValidEndpointAddressAsMapValue() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("EndpointAddress", NON_VALID_URL);
        new InProcessTelemetryChannel(map);
    }
    @Test()
    public void testStringIntegerMaxInstanceRetry() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(INSTANT_RETRY_NAME, "AABB");
        new InProcessTelemetryChannel(map);
    }

    @Test()
    public void testValidIntegerMaxInstanceRetry() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(INSTANT_RETRY_NAME, "4");
        new InProcessTelemetryChannel(map);
    }

    @Test()
    public void testInvalidIntegerMaxInstanceRetry() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(INSTANT_RETRY_NAME, "-1");
        new InProcessTelemetryChannel(map);
    }

    @Test
    public void testInProcessTelemetryChannelWithDefaultSpringBootParameters() {
        new InProcessTelemetryChannel("https://dc.services.visualstudio.com/v2/track", "10",
                false, 500, 5, true);
    }
}