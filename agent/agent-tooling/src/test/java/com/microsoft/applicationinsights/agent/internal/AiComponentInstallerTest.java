package com.microsoft.applicationinsights.agent.internal;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AiComponentInstallerTest {

    @Test
    public void jmxObjectNameCreation() {
        assertEquals("java.lang / Threading / ", AiComponentInstaller.getJmxDisplayName("java.lang:type=Threading"));
        assertEquals("java.nio / BufferPool / direct / ", AiComponentInstaller.getJmxDisplayName("java.nio:type=BufferPool,name=direct"));
        assertEquals("java.lang / GarbageCollector / PS MarkSweep / ", AiComponentInstaller.getJmxDisplayName("java.lang:type=GarbageCollector,name=PS MarkSweep"));
        assertEquals("Catalina / RequestProcessor / \"http-nio-8080\" / HttpRequest1 / ", AiComponentInstaller.getJmxDisplayName("Catalina:type=RequestProcessor,worker=\"http-nio-8080\",name=HttpRequest1"));
    }
}
