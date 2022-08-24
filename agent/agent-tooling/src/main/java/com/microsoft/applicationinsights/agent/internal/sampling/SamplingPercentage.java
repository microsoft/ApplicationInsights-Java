/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
