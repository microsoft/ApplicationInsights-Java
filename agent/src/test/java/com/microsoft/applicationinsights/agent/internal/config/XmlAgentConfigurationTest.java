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

package com.microsoft.applicationinsights.agent.internal.config;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public final class XmlAgentConfigurationTest {
    @Test
    public void testCtor() {
        AgentConfigurationDefaultImpl tested = new AgentConfigurationDefaultImpl();
        assertFalse(tested.getBuiltInConfiguration().isEnabled());
        assertNull(tested.getRequestedClassesToInstrument());
    }

    @Test
    public void testSetters() {
        AgentConfigurationDefaultImpl tested = new AgentConfigurationDefaultImpl();
        HashMap<String, ClassInstrumentationData> classes = new HashMap<String, ClassInstrumentationData>();
        tested.setRequestedClassesToInstrument(classes);

        assertSame(classes, tested.getRequestedClassesToInstrument());
    }
}