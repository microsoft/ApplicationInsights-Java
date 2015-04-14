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

        assertEquals(context.getId(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceId()), "mock");
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

        assertEquals(context.getModel(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceModel()), "mock");
    }

    @Test
    public void testSetNetworkType() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setNetworkType("mock");

        assertEquals(context.getNetworkType(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceNetwork()), "mock");
    }

    @Test
    public void testSetOemName() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setOemName("mock");

        assertEquals(context.getOemName(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceOEMName()), "mock");
    }

    @Test
    public void testSetOperatingSystem() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setOperatingSystem("mock");

        assertEquals(context.getOperatingSystem(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceOS()), "mock");
    }

    @Test
    public void testSetOperatingSystemVersion() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setOperatingSystemVersion("mock");

        assertEquals(context.getOperatingSystemVersion(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceOSVersion()), "mock");
    }

    @Test
    public void testSetRoleInstance() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setRoleInstance("mock");

        assertEquals(context.getRoleInstance(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceRoleInstance()), "mock");
    }

    @Test
    public void testSetRoleName() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setRoleName("mock");

        assertEquals(context.getRoleName(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceRoleName()), "mock");
    }

    @Test
    public void testSetScreenResolution() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        DeviceContext context = new DeviceContext(map);
        context.setScreenResolution("mock");

        assertEquals(context.getScreenResolution(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getDeviceScreenResolution()), "mock");
    }
}
