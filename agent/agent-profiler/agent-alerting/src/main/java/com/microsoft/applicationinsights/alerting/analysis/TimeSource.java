// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis;

import java.time.Instant;

/** Source of time that may be overridden for tests. */
public abstract class TimeSource {
  public abstract Instant getNow();

  public static final TimeSource DEFAULT =
      new TimeSource() {
        @Override
        public Instant getNow() {
          return Instant.now();
        }
      };
}
