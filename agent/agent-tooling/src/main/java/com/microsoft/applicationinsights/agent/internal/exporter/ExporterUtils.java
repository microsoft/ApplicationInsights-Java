// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import com.microsoft.applicationinsights.agent.internal.sampling.AiSampler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.trace.SpanContext;
import java.util.concurrent.ThreadLocalRandom;

public final class ExporterUtils {

  @SuppressFBWarnings(
      value = "SECPR", // Predictable pseudorandom number generator
      justification = "Predictable random is ok for sampling decision")
  public static boolean shouldSample(SpanContext spanContext, double percentage) {
    if (percentage == 100) {
      // optimization, no need to calculate score
      return true;
    }
    if (percentage == 0) {
      // optimization, no need to calculate score
      return false;
    }
    if (spanContext.isValid()) {
      return AiSampler.shouldRecordAndSample(spanContext.getTraceId(), percentage);
    }
    // this is a standalone log (not part of a trace), so randomly sample at the given percentage
    return ThreadLocalRandom.current().nextDouble() < percentage / 100;
  }

  private ExporterUtils() {}
}
