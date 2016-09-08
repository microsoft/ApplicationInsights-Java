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

package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.DeviceInfoContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.SdkVersionContextInitializer;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by gupele on 9/8/2016.
 */
public class ContextInitializersInitializerTest {

    @Test
    public void testInitialize() {
        ContextInitializersXmlElement xmlElement = new ContextInitializersXmlElement();

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        ContextInitializersInitializer tested = new ContextInitializersInitializer();
        tested.initialize(xmlElement, mockConfiguration);

        List<ContextInitializer> initializerList = mockConfiguration.getContextInitializers();
        assertEquals(initializerList.size(), 2);

        ContextInitializer initializer = initializerList.get(0);
        if (initializer instanceof SdkVersionContextInitializer) {
            assertTrue(initializerList.get(1) instanceof DeviceInfoContextInitializer);
        } else {
            assertTrue(initializer instanceof DeviceInfoContextInitializer);
            assertTrue(initializerList.get(1) instanceof SdkVersionContextInitializer);
        }
    }

}