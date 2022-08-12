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

package io.opentelemetry.instrumentation.api.instrumenter.rpc;

import static io.opentelemetry.instrumentation.api.instrumenter.rpc.MetricsView.applyServerView;
import static io.opentelemetry.instrumentation.api.instrumenter.utils.DurationBucketizer.AI_PERFORMANCE_BUCKET;
import static java.util.logging.Level.FINE;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.utils.DurationBucketizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link OperationListener} which keeps track of <a
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#rpc-server">RPC
 * server metrics</a>.
 */
public final class RpcServerMetrics implements OperationListener {

  private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final ContextKey<RpcServerMetrics.State> RPC_SERVER_REQUEST_METRICS_STATE =
      ContextKey.named("rpc-server-request-metrics-state");

  private static final Logger logger = Logger.getLogger(RpcServerMetrics.class.getName());

  private final DoubleHistogram serverDurationHistogram;

  private RpcServerMetrics(Meter meter) {
    serverDurationHistogram =
        meter
            .histogramBuilder("rpc.server.duration")
            .setDescription("The duration of an inbound RPC invocation")
            .setUnit("ms")
            .build();
  }

  /**
   * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
   * RpcServerMetrics} on an {@link
   * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
   */
  public static OperationMetrics get() {
    return RpcServerMetrics::new;
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        RPC_SERVER_REQUEST_METRICS_STATE,
        new AutoValue_RpcServerMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(RPC_SERVER_REQUEST_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record RPC request metrics.",
          context);
      return;
    }

    double duration = (endNanos - state.startTimeNanos()) / NANOS_PER_MS;
    System.out.println("##################### server.duration: " + duration);
    System.out.println(
        "##################### server.perfBucket: "
            + DurationBucketizer.getPerformanceBucket(duration));
    endAttributes =
        endAttributes.toBuilder()
            .put(AI_PERFORMANCE_BUCKET, DurationBucketizer.getPerformanceBucket(duration))
            .build();

    serverDurationHistogram.record(
        duration, applyServerView(state.startAttributes(), endAttributes), context);
  }

  @AutoValue
  abstract static class State {

    abstract Attributes startAttributes();

    abstract long startTimeNanos();
  }
}
