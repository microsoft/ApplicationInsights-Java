// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.logbackpatch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FileNamePatternTest {

  @Test
  public void test() {
    assertThat(FileNamePattern.escapeDirectory("/test%20-test/filename%i.log"))
        .isEqualTo("/test\\%20-test/filename%i.log");
  }
}
