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

import static io.opentelemetry.instrumentation.api.instrumenter.UserAgents.IS_PRE_AGGREGATED;
import static io.opentelemetry.instrumentation.api.instrumenter.UserAgents.IS_SYNTHETIC;
import static io.opentelemetry.instrumentation.api.instrumenter.UserAgents.TARGET;
import static io.opentelemetry.instrumentation.api.instrumenter.UserAgents.isUserAgentBot;
import static io.opentelemetry.instrumentation.api.instrumenter.rpc.MetricsView.applyClientView;
import static java.util.logging.Level.FINE;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * {@link OperationListener} which keeps track of <a
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#rpc-client">RPC
 * client metrics</a>.
 */
public final class RpcClientMetrics implements OperationListener {

  private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final ContextKey<RpcClientMetrics.State> RPC_CLIENT_REQUEST_METRICS_STATE =
      ContextKey.named("rpc-client-request-metrics-state");

  private static final Logger logger = Logger.getLogger(RpcClientMetrics.class.getName());

  private final DoubleHistogram clientDurationHistogram;

  private RpcClientMetrics(Meter meter) {
    clientDurationHistogram =
        meter
            .histogramBuilder("rpc.client.duration")
            .setDescription("The duration of an outbound RPC invocation")
            .setUnit("ms")
            .build();
  }

  /**
   * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
   * RpcClientMetrics} on an {@link
   * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
   */
  public static OperationMetrics get() {
    return RpcClientMetrics::new;
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        RPC_CLIENT_REQUEST_METRICS_STATE,
        new AutoValue_RpcClientMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(RPC_CLIENT_REQUEST_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record RPC request metrics.",
          context);
      return;
    }

    // START APPLICATION INSIGHTS CODE

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    Span.fromContext(context).setAttribute(IS_PRE_AGGREGATED, "True");

    String target = getTargetFromPeerAttributes(endAttributes, 0);
    if (target == null) {
      target = endAttributes.get(SemanticAttributes.RPC_SYSTEM);
    }
    endAttributes =
        endAttributes.toBuilder()
            .put(IS_SYNTHETIC, isUserAgentBot(endAttributes, state.startAttributes()))
            .put(TARGET, target)
            .build();
    // END APPLICATION INSIGHTS CODE

    clientDurationHistogram.record(
        (endNanos - state.startTimeNanos()) / NANOS_PER_MS,
        applyClientView(state.startAttributes(), endAttributes),
        context);
  }

  @Nullable
  private static String getTargetFromPeerAttributes(Attributes attributes, int defaultPort) {
    String target = getTargetFromPeerService(attributes);
    if (target != null) {
      return target;
    }
    return getTargetFromNetAttributes(attributes, defaultPort);
  }

  @Nullable
  private static String getTargetFromPeerService(Attributes attributes) {
    // do not append port to peer.service
    return attributes.get(SemanticAttributes.PEER_SERVICE);
  }

  @Nullable
  private static String getTargetFromNetAttributes(Attributes attributes, int defaultPort) {
    String target = getHostFromNetAttributes(attributes);
    if (target == null) {
      return null;
    }
    // append net.peer.port to target
    Long port = attributes.get(SemanticAttributes.NET_PEER_PORT);
    if (port != null && port != defaultPort) {
      return target + ":" + port;
    }
    return target;
  }

  @Nullable
  private static String getHostFromNetAttributes(Attributes attributes) {
    String host = attributes.get(SemanticAttributes.NET_PEER_NAME);
    if (host != null) {
      return host;
    }
    return attributes.get(SemanticAttributes.NET_PEER_IP);
  }

  @AutoValue
  abstract static class State {

    abstract Attributes startAttributes();

    abstract long startTimeNanos();
  }
}
