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

package com.microsoft.applicationinsights.web.internal;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.web.extensibility.modules.WebSessionTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule;

/**
 * Created by yonisha on 3/9/2015.
 * TODO:
 * 1. Add test to verify that modules invocation continues when exception is thrown from a single module.
 */
public class WebModulesContainerTests {

    private WebModulesContainer container;

    // region Initialization

    @Before
    public void testInitialize() {
        container = new WebModulesContainer(TelemetryConfiguration.getActive());
    }

    // endregion Initialization

    // region Tests

    @Test
    public void testAllTelemetryModulesAreLoaded() {
        Assert.assertEquals(3, container.getModulesCount());
    }

    @Test
    public void testWhenUserModuleEnabledSessionModuleIsUpdated() throws Exception {
        WebSessionTrackingTelemetryModule sessionModule = getModuleByType(WebSessionTrackingTelemetryModule.class);

        Assert.assertTrue(sessionModule.getIsUserModuleEnabled());
    }

    // endregion Tests

    // region Private

    private <T> T getModuleByType(Class<T> tClass) throws Exception {

        T module = null;
        List<WebTelemetryModule> containerModules = getContainerModules();

        for (WebTelemetryModule telemetryModule : containerModules) {
            if (tClass.isInstance(telemetryModule)) {
                module = tClass.cast(telemetryModule);
                break;
            }
        }

        return module;
    }

    @SuppressWarnings("unchecked")
    private List<WebTelemetryModule> getContainerModules() throws Exception {
        Field field = container.getClass().getDeclaredField("modules");
        field.setAccessible(true);

        return (List<WebTelemetryModule>)field.get(container);
    }

    // endregion Private
}
