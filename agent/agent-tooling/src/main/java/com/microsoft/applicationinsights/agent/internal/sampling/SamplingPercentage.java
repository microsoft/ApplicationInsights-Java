// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder;

// all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, ...)
// e.g. 50 for 1/2 or 33.33 for 1/3
//
// failure to follow this pattern can result in unexpected / incorrect computation of values in
// the portal
public interface SamplingPercentage {

  double get();

  static SamplingPercentage fixed(double percentage) {
    double roundedPercentage = ConfigurationBuilder.roundToNearest(percentage);
    return () -> roundedPercentage;
  }

  static SamplingPercentage rateLimited(double targetPerSecondLimit) {
    return new RateLimitedSamplingPercentage(targetPerSecondLimit, 0.1);
  }
}
