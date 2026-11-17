// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExecutedMonikerTrackerTest {

  @Test
  void rejectsDuplicateWithinRetentionWindow() {
    ExecutedMonikerTracker tracker = new ExecutedMonikerTracker(Duration.ofMinutes(10), 10);
    Instant now = Instant.parse("2026-07-24T13:58:12Z");

    assertThat(tracker.tryMarkExecuted("Portal_test", now)).isTrue();
    assertThat(tracker.tryMarkExecuted("Portal_test", now.plusSeconds(60))).isFalse();
    assertThat(tracker.tryMarkExecuted("Portal_test", now.plusSeconds(601))).isTrue();
  }

  @Test
  void evictsOldestEntryAtCapacity() {
    ExecutedMonikerTracker tracker = new ExecutedMonikerTracker(Duration.ofMinutes(10), 2);
    Instant now = Instant.parse("2026-07-24T13:58:12Z");

    assertThat(tracker.tryMarkExecuted("one", now)).isTrue();
    assertThat(tracker.tryMarkExecuted("two", now)).isTrue();
    assertThat(tracker.tryMarkExecuted("three", now)).isTrue();
    assertThat(tracker.tryMarkExecuted("one", now.plusSeconds(1))).isTrue();
  }

  @Test
  void rejectsBlankMoniker() {
    ExecutedMonikerTracker tracker = new ExecutedMonikerTracker(Duration.ofMinutes(10), 10);
    Instant now = Instant.parse("2026-07-24T13:58:12Z");

    assertThat(tracker.tryMarkExecuted(null, now)).isFalse();
    assertThat(tracker.tryMarkExecuted("  ", now)).isFalse();
  }
}
