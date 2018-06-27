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

package com.microsoft.applicationinsights.internal.channel.sampling;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.util.Random;

/**
 * Created by gupele on 11/2/2016.
 *
 * <p>Utility class for sampling score generation.
 */
final class SamplingScoreGenerator {
  private static Random rand = new Random();

  public static double getSamplingScore(Telemetry telemetry) {
    double samplingScore = 0;

    try {
      if (telemetry.getContext().getUser().getId() != null) {
        samplingScore =
            (double) getSamplingHashCode(telemetry.getContext().getUser().getId())
                / Integer.MAX_VALUE;
      } else if (telemetry.getContext().getOperation().getId() != null) {
        samplingScore =
            (double) getSamplingHashCode(telemetry.getContext().getOperation().getId())
                / Integer.MAX_VALUE;
      } else {
        samplingScore = rand.nextDouble();
      }
    } catch (Exception e) {
      InternalLogger.INSTANCE.error("Failed to fetch sample number for telemetry, using default");
      samplingScore = rand.nextDouble();
    }

    samplingScore *= 100;
    return samplingScore;
  }

  private static int getSamplingHashCode(String input) {
    if (input == null) {
      return 0;
    }

    while (input.length() < 8) {
      input = input + input;
    }

    int hash = 5381;

    char[] asChars = input.toCharArray();
    for (int i = 0; i < asChars.length; i++) {
      hash = ((hash << 5) + hash) + (int) asChars[i];
    }

    return hash == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(hash);
  }
}
