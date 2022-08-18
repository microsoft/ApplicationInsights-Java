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

package io.opentelemetry.instrumentation.api.instrumenter.http;

import static io.opentelemetry.instrumentation.api.instrumenter.Utils.IS_SYNTHETIC;
import static io.opentelemetry.instrumentation.api.instrumenter.Utils.TARGET;
import static io.opentelemetry.instrumentation.api.instrumenter.Utils.isUserAgentBot;
import static io.opentelemetry.instrumentation.api.instrumenter.http.TemporaryMetricsView.applyClientDurationAndSizeView;
import static java.util.logging.Level.FINE;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
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
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/http-metrics.md#http-client">HTTP
 * client metrics</a>.
 */
public final class HttpClientMetrics implements OperationListener {

  private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final ContextKey<State> HTTP_CLIENT_REQUEST_METRICS_STATE =
      ContextKey.named("http-client-request-metrics-state");

  private static final Logger logger = Logger.getLogger(HttpClientMetrics.class.getName());

  /**
   * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
   * HttpClientMetrics} on an {@link
   * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
   */
  public static OperationMetrics get() {
    return HttpClientMetrics::new;
  }

  private final DoubleHistogram duration;
  private final LongHistogram requestSize;
  private final LongHistogram responseSize;

  private HttpClientMetrics(Meter meter) {
    duration =
        meter
            .histogramBuilder("http.client.duration")
            .setUnit("ms")
            .setDescription("The duration of the outbound HTTP request")
            .build();
    requestSize =
        meter
            .histogramBuilder("http.client.request.size")
            .setUnit("By")
            .setDescription("The size of HTTP request messages")
            .ofLongs()
            .build();
    responseSize =
        meter
            .histogramBuilder("http.client.response.size")
            .setUnit("By")
            .setDescription("The size of HTTP response messages")
            .ofLongs()
            .build();
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        HTTP_CLIENT_REQUEST_METRICS_STATE,
        new AutoValue_HttpClientMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(HTTP_CLIENT_REQUEST_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record HTTP request metrics.",
          context);
      return;
    }
    Attributes durationAndSizeAttributes =
        applyClientDurationAndSizeView(state.startAttributes(), endAttributes);

    Attributes durationAttributes =
        durationAndSizeAttributes.toBuilder()
            .put(
                IS_SYNTHETIC,
                String.valueOf(isUserAgentBot(endAttributes, state.startAttributes())))
            .put(TARGET, getTargetForHttpClientSpan(durationAndSizeAttributes))
            .build();
    ;
    this.duration.record(
        (endNanos - state.startTimeNanos()) / NANOS_PER_MS, durationAttributes, context);

    Long requestLength =
        getAttribute(
            SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, endAttributes, state.startAttributes());
    if (requestLength != null) {
      requestSize.record(requestLength, durationAndSizeAttributes);
    }
    Long responseLength =
        getAttribute(
            SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH,
            endAttributes,
            state.startAttributes());
    if (responseLength != null) {
      responseSize.record(responseLength, durationAndSizeAttributes);
    }
  }

  private static String getTargetForHttpClientSpan(Attributes attributes) {
    // from the spec, at least one of the following sets of attributes is required:
    // * http.url
    // * http.scheme, http.host, http.target
    // * http.scheme, net.peer.name, net.peer.port, http.target
    // * http.scheme, net.peer.ip, net.peer.port, http.target
    String target = getTargetFromPeerService(attributes);
    if (target != null) {
      return target;
    }
    // note http.host includes the port (at least when non-default)
    target = attributes.get(SemanticAttributes.HTTP_HOST);
    if (target != null) {
      String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
      if ("http".equals(scheme)) {
        if (target.endsWith(":80")) {
          target = target.substring(0, target.length() - 3);
        }
      } else if ("https".equals(scheme)) {
        if (target.endsWith(":443")) {
          target = target.substring(0, target.length() - 4);
        }
      }
      return target;
    }
    String url = attributes.get(SemanticAttributes.HTTP_URL);
    if (url != null) {
      target = getTargetFromUrl(url);
      if (target != null) {
        return target;
      }
    }
    String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
    int defaultPort;
    if ("http".equals(scheme)) {
      defaultPort = 80;
    } else if ("https".equals(scheme)) {
      defaultPort = 443;
    } else {
      defaultPort = 0;
    }
    target = getTargetFromNetAttributes(attributes, defaultPort);
    if (target != null) {
      return target;
    }
    // this should not happen, just a failsafe
    return "Http";
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

  @Nullable
  private static String getTargetFromUrl(String url) {
    int schemeEndIndex = url.indexOf(':');
    if (schemeEndIndex == -1) {
      // not a valid url
      return null;
    }

    int len = url.length();
    if (schemeEndIndex + 2 < len
        && url.charAt(schemeEndIndex + 1) == '/'
        && url.charAt(schemeEndIndex + 2) == '/') {
      // has authority component
      // look for
      //   '/' - start of path
      //   '?' or end of string - empty path
      int index;
      for (index = schemeEndIndex + 3; index < len; index++) {
        char c = url.charAt(index);
        if (c == '/' || c == '?' || c == '#') {
          break;
        }
      }
      String target = url.substring(schemeEndIndex + 3, index);
      return target.isEmpty() ? null : target;
    } else {
      // has no authority
      return null;
    }
  }

  @Nullable
  private static <T> T getAttribute(AttributeKey<T> key, Attributes... attributesList) {
    for (Attributes attributes : attributesList) {
      T value = attributes.get(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  @AutoValue
  abstract static class State {

    abstract Attributes startAttributes();

    abstract long startTimeNanos();
  }
}
