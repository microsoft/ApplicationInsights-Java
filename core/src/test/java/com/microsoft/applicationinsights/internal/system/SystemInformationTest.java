package com.microsoft.applicationinsights.internal.system;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SystemInformationTest {
    @Test
    public void testOS() {
        assertTrue(SystemUtils.IS_OS_WINDOWS ? SystemInformation.INSTANCE.isWindows() : SystemInformation.INSTANCE.isUnix());
    }

    @Test
    public void testProcessId() {
        Integer.parseInt(SystemInformation.INSTANCE.getProcessId());
    }
}
