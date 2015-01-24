/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class LocationContextTest {
    @Test
    public void testSetBadIp() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        LocationContext context = new LocationContext(map);
        context.setIp("a.255.255.255");

        assertNull(context.getIp());
        assertEquals(map.size(), 0);
        assertNull(map.get(ContextTagKeys.getKeys().getLocationIP()));
    }

    @Test
    public void testSetIp() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        LocationContext context = new LocationContext(map);
        context.setIp("127.255.255.255");

        assertEquals(context.getIp(), "127.255.255.255");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getLocationIP()), "127.255.255.255");
    }
}