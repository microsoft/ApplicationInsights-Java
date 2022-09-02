// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.microsoft.applicationinsights.agent.internal.sampling;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

// uses adaptive algorithm from OpenTelemetry Java Contrib's ConsistentRateLimitingSampler
// (https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/consistent-sampling/src/main/java/io/opentelemetry/contrib/samplers/ConsistentRateLimitingSampler.java)
class RateLimitedSamplingPercentage implements SamplingPercentage {

  private static final class State {
    private final double effectiveWindowCount;
    private final double effectiveWindowNanos;
    private final long lastNanoTime;

    private State(double effectiveWindowCount, double effectiveWindowNanos, long lastNanoTime) {
      this.effectiveWindowCount = effectiveWindowCount;
      this.effectiveWindowNanos = effectiveWindowNanos;
      this.lastNanoTime = lastNanoTime;
    }
  }

  private final LongSupplier nanoTimeSupplier;
  private final double inverseAdaptationTimeNanos;
  private final double targetSpansPerNanosecondLimit;
  private final AtomicReference<State> state;
  private final boolean roundToNearest;

  RateLimitedSamplingPercentage(double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    this(targetSpansPerSecondLimit, adaptationTimeSeconds, System::nanoTime, true);
  }

  // visible for testing
  RateLimitedSamplingPercentage(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      LongSupplier nanoTimeSupplier,
      boolean roundToNearest) {

    if (targetSpansPerSecondLimit < 0.0) {
      throw new IllegalArgumentException("Limit for sampled spans per second must be nonnegative!");
    }
    if (adaptationTimeSeconds < 0.0) {
      throw new IllegalArgumentException("Adaptation rate must be nonnegative!");
    }
    this.nanoTimeSupplier = requireNonNull(nanoTimeSupplier);

    this.inverseAdaptationTimeNanos = 1e-9 / adaptationTimeSeconds;
    this.targetSpansPerNanosecondLimit = 1e-9 * targetSpansPerSecondLimit;

    this.state = new AtomicReference<>(new State(0, 0, nanoTimeSupplier.getAsLong()));

    this.roundToNearest = roundToNearest;
  }

  private State updateState(State oldState, long currentNanoTime) {
    if (currentNanoTime <= oldState.lastNanoTime) {
      return new State(
          oldState.effectiveWindowCount + 1, oldState.effectiveWindowNanos, oldState.lastNanoTime);
    }
    long nanoTimeDelta = currentNanoTime - oldState.lastNanoTime;
    double decayFactor = Math.exp(-nanoTimeDelta * inverseAdaptationTimeNanos);
    double currentEffectiveWindowCount = oldState.effectiveWindowCount * decayFactor + 1;
    double currentEffectiveWindowNanos =
        oldState.effectiveWindowNanos * decayFactor + nanoTimeDelta;
    return new State(currentEffectiveWindowCount, currentEffectiveWindowNanos, currentNanoTime);
  }

  @Override
  public double get() {
    long currentNanoTime = nanoTimeSupplier.getAsLong();
    State currentState = state.updateAndGet(s -> updateState(s, currentNanoTime));

    double samplingProbability =
        (currentState.effectiveWindowNanos * targetSpansPerNanosecondLimit)
            / currentState.effectiveWindowCount;

    double samplingPercentage = 100 * Math.min(samplingProbability, 1);

    if (roundToNearest) {
      samplingPercentage = roundDownToNearest(samplingPercentage);
    }
    return samplingPercentage;
  }

  private static double roundDownToNearest(double samplingPercentage) {
    if (samplingPercentage == 0) {
      return 0;
    }
    double itemCount = 100 / samplingPercentage;
    return 100.0 / Math.ceil(itemCount);
  }
}
