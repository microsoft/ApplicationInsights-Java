// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import org.junit.jupiter.api.Test;

class FirstEntryPointTest {
  @Test
  void getFriendlyExceptionTest() {
    FriendlyException friendlyException =
        FirstEntryPoint.getFriendlyException(new FriendlyException("<message>", "<action>"));
    FriendlyException nonFriendlyException =
        FirstEntryPoint.getFriendlyException(new IllegalArgumentException());
    FriendlyException nestedFriendlyException =
        FirstEntryPoint.getFriendlyException(
            new RuntimeException(
                "Run time Exception", new FriendlyException("<message>", "<action>")));
    FriendlyException nestedNonFriendlyException =
        FirstEntryPoint.getFriendlyException(
            new RuntimeException("Run time Exception", new IllegalArgumentException()));
    assertThat(friendlyException).isNotNull();
    assertThat(nonFriendlyException).isNull();
    assertThat(nestedFriendlyException).isNotNull();
    assertThat(nestedNonFriendlyException).isNull();
  }
}
