// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import org.junit.jupiter.api.Test;

public class StringsTest {

  @Test
  void testEmptyToNull() {
    assertThat(Strings.trimAndEmptyToNull("   ")).isNull();
    assertThat(Strings.trimAndEmptyToNull("")).isNull();
    assertThat(Strings.trimAndEmptyToNull(null)).isNull();
    assertThat(Strings.trimAndEmptyToNull("a")).isEqualTo("a");
    assertThat(Strings.trimAndEmptyToNull("  a  ")).isEqualTo("a");
    assertThat(Strings.trimAndEmptyToNull("\t")).isNull();
  }
}
