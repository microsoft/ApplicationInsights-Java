package com.microsoft.applicationinsights.internal.system;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemInformationTest {
    @Test
    public void testOS() {
        assertThat(SystemUtils.IS_OS_WINDOWS ? SystemInformation.INSTANCE.isWindows() : SystemInformation.INSTANCE.isUnix()).isTrue();
    }

    @Test
    public void testProcessId() {
        Integer.parseInt(SystemInformation.INSTANCE.getProcessId());
    }
}
