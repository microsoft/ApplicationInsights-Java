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

public final class DeviceContextTest {
    @Test
    public void testSetId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setId("mock");

        assertEquals("mock", context.getId());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceId()));
    }

    @Test
    public void testSetLanguage() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setLanguage("mock");

        assertEquals(context.getLanguage(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceLanguage()), "mock");
    }

    @Test
    public void testSetModel() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setModel("mock");

        assertEquals("mock", context.getModel());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceModel()));
    }

    @Test
    public void testSetNetworkType() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setNetworkType("mock");

        assertEquals("mock", context.getNetworkType());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceNetwork()));
    }

    @Test
    public void testSetOemName() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setOemName("mock");

        assertEquals("mock", context.getOemName());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceOEMName()));
    }

    @Test
    public void testSetOperatingSystem() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setOperatingSystem("mock");

        assertEquals("mock", context.getOperatingSystem());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceOS()));
    }

    @Test
    public void testSetOperatingSystemVersion() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setOperatingSystemVersion("mock");

        assertEquals("mock", context.getOperatingSystemVersion());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceOSVersion()));
    }

    @Test
    public void testSetRoleInstance() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setRoleInstance("mock");

        assertEquals("mock", context.getRoleInstance());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceRoleInstance()));
    }

    @Test
    public void testSetRoleName() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setRoleName("mock");

        assertEquals("mock", context.getRoleName());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceRoleName()));
    }

    @Test
    public void testSetScreenResolution() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setScreenResolution("mock");

        assertEquals("mock", context.getScreenResolution());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getDeviceScreenResolution()));
    }
}
