// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.SystemInformation;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

class SystemInformationTest {
  @Test
  void testOs() {
    assertThat(
            SystemUtils.IS_OS_WINDOWS ? SystemInformation.isWindows() : SystemInformation.isLinux())
        .isTrue();
  }

  @Test
  void testProcessId() {
    Integer.parseInt(SystemInformation.getProcessId());
  }
}
