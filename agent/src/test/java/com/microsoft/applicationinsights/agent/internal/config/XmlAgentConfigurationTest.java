package com.microsoft.applicationinsights.agent.internal.config;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public final class XmlAgentConfigurationTest {
    @Test
    public void testCtor() {
        XmlAgentConfiguration tested = new XmlAgentConfiguration();
        assertTrue(tested.getBuiltInSwitches().isEnabled());
        assertNull(tested.getRequestedClassesToInstrument());
    }

    @Test
    public void testSetters() {
        XmlAgentConfiguration tested = new XmlAgentConfiguration();
        HashMap<String, ClassInstrumentationData> classes = new HashMap<String, ClassInstrumentationData>();
        tested.setRequestedClassesToInstrument(classes);

        assertSame(classes, tested.getRequestedClassesToInstrument());
    }
}