package com.microsoft.applicationinsights.internal.system;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public final class SystemInformationTest {
    @Test
    public void testOS() {
        assertTrue(SystemUtils.IS_OS_WINDOWS ? SystemInformation.INSTANCE.isWindows() : SystemInformation.INSTANCE.isUnix());
    }

    @Test
    public void testProcessId() {
        Integer.parseInt(SystemInformation.INSTANCE.getProcessId());
    }
}
