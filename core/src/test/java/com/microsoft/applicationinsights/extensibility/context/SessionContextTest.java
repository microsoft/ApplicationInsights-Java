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

package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class SessionContextTest {
    @Test
    public void testSetId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setId("mock");

        assertEquals(context.getId(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getSessionId()), "mock");
    }

    @Test
    public void testSetIsFirstNull() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsFirst(null);

        assertNull(context.getIsFirst());
        assertEquals(map.size(), 0);
        assertNull(map.get(ContextTagKeys.getKeys().getSessionIsFirst()));
    }

    @Test
    public void testSetIsFirstTrue() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsFirst(true);

        assertEquals(context.getIsFirst(), true);
        assertEquals(map.size(), 1);
        assertEquals(Boolean.valueOf(map.get(ContextTagKeys.getKeys().getSessionIsFirst())), true);
    }

    @Test
    public void testSetIsNewSessionNull() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsNewSession(null);

        assertNull(context.getIsNewSession());
        assertEquals(map.size(), 0);
        assertNull(map.get(ContextTagKeys.getKeys().getSessionIsNew()));
    }

    @Test
    public void testSetIsNewSessionTrue() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsNewSession(true);

        assertEquals(context.getIsNewSession(), true);
        assertEquals(map.size(), 1);
        assertEquals(Boolean.valueOf(map.get(ContextTagKeys.getKeys().getSessionIsNew())), true);
    }
}