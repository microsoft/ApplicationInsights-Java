/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.api.aiappid;

import static io.opentelemetry.internal.Utils.checkArgument;
import static io.opentelemetry.internal.Utils.checkNotNull;

import io.grpc.Context;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.DefaultSpan;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import io.opentelemetry.trace.TracingContextUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// copy of io.opentelemetry.trace.propagation.HttpTraceContext from OpenTelemetry API 0.3.0
// that also injects ApplicationInsight's appId into tracestate
public class AiHttpTraceContext implements HttpTextFormat {
  private static final Logger logger = Logger.getLogger(AiHttpTraceContext.class.getName());

  private static final TraceState TRACE_STATE_DEFAULT = TraceState.builder().build();
  static final String TRACE_PARENT = "traceparent";
  static final String TRACE_STATE = "tracestate";
  private static final List<String> FIELDS =
      Collections.unmodifiableList(Arrays.asList(TRACE_PARENT, TRACE_STATE));

  private static final String VERSION = "00";
  private static final int VERSION_SIZE = 2;
  private static final char TRACEPARENT_DELIMITER = '-';
  private static final int TRACEPARENT_DELIMITER_SIZE = 1;
  private static final int TRACE_ID_HEX_SIZE = 2 * TraceId.getSize();
  private static final int SPAN_ID_HEX_SIZE = 2 * SpanId.getSize();
  private static final int TRACE_OPTION_HEX_SIZE = 2 * TraceFlags.getSize();
  private static final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int SPAN_ID_OFFSET =
      TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACE_OPTION_OFFSET =
      SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACEPARENT_HEADER_SIZE = TRACE_OPTION_OFFSET + TRACE_OPTION_HEX_SIZE;
  private static final int TRACESTATE_MAX_SIZE = 512;
  private static final int TRACESTATE_MAX_MEMBERS = 32;
  private static final char TRACESTATE_KEY_VALUE_DELIMITER = '=';
  private static final char TRACESTATE_ENTRY_DELIMITER = ',';
  private static final Pattern TRACESTATE_ENTRY_DELIMITER_SPLIT_PATTERN =
      Pattern.compile("[ \t]*" + TRACESTATE_ENTRY_DELIMITER + "[ \t]*");

  @Override
  public List<String> fields() {
    return FIELDS;
  }

  @Override
  public <C> void inject(final Context context, final C carrier, final Setter<C> setter) {
    checkNotNull(context, "context");
    checkNotNull(setter, "setter");
    checkNotNull(carrier, "carrier");

    final Span span = TracingContextUtils.getSpanWithoutDefault(context);
    if (span == null) {
      return;
    }

    injectImpl(span.getContext(), carrier, setter);
  }

  private static <C> void injectImpl(
      final SpanContext spanContext, final C carrier, final Setter<C> setter) {
    final char[] chars = new char[TRACEPARENT_HEADER_SIZE];
    chars[0] = VERSION.charAt(0);
    chars[1] = VERSION.charAt(1);
    chars[2] = TRACEPARENT_DELIMITER;
    spanContext.getTraceId().copyLowerBase16To(chars, TRACE_ID_OFFSET);
    chars[SPAN_ID_OFFSET - 1] = TRACEPARENT_DELIMITER;
    spanContext.getSpanId().copyLowerBase16To(chars, SPAN_ID_OFFSET);
    chars[TRACE_OPTION_OFFSET - 1] = TRACEPARENT_DELIMITER;
    spanContext.getTraceFlags().copyLowerBase16To(chars, TRACE_OPTION_OFFSET);
    setter.set(carrier, TRACE_PARENT, new String(chars));
    final List<TraceState.Entry> entries = spanContext.getTraceState().getEntries();
    final String appId = AiAppId.getAppId();
    if (entries.isEmpty() && appId.isEmpty()) {
      // No need to add an empty "tracestate" header.
      return;
    }
    final StringBuilder stringBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);
    if (!appId.isEmpty()) {
      stringBuilder
          .append(AiAppId.TRACESTATE_KEY)
          .append(TRACESTATE_KEY_VALUE_DELIMITER)
          .append(appId);
    }
    for (final TraceState.Entry entry : entries) {
      if (stringBuilder.length() != 0) {
        stringBuilder.append(TRACESTATE_ENTRY_DELIMITER);
      }
      final String key = entry.getKey();
      if (!AiAppId.TRACESTATE_KEY.equals(key)) {
        stringBuilder.append(key).append(TRACESTATE_KEY_VALUE_DELIMITER).append(entry.getValue());
      }
    }
    setter.set(carrier, TRACE_STATE, stringBuilder.toString());
  }

  @Override
  public <C /*>>> extends @NonNull Object*/> Context extract(
      final Context context, final C carrier, final Getter<C> getter) {
    checkNotNull(carrier, "context");
    checkNotNull(carrier, "carrier");
    checkNotNull(getter, "getter");

    final SpanContext spanContext = extractImpl(carrier, getter);
    return TracingContextUtils.withSpan(DefaultSpan.create(spanContext), context);
  }

  private static <C> SpanContext extractImpl(final C carrier, final Getter<C> getter) {
    final String traceparent = getter.get(carrier, TRACE_PARENT);
    if (traceparent == null) {
      return SpanContext.getInvalid();
    }

    final SpanContext contextFromParentHeader = extractContextFromTraceParent(traceparent);
    if (!contextFromParentHeader.isValid()) {
      return contextFromParentHeader;
    }

    final String traceStateHeader = getter.get(carrier, TRACE_STATE);
    if (traceStateHeader == null || traceStateHeader.isEmpty()) {
      return contextFromParentHeader;
    }

    try {
      final TraceState traceState = extractTraceState(traceStateHeader);
      return SpanContext.createFromRemoteParent(
          contextFromParentHeader.getTraceId(),
          contextFromParentHeader.getSpanId(),
          contextFromParentHeader.getTraceFlags(),
          traceState);
    } catch (final IllegalArgumentException e) {
      logger.info("Unparseable tracestate header. Returning span context without state.");
      return contextFromParentHeader;
    }
  }

  private static SpanContext extractContextFromTraceParent(final String traceparent) {
    // TODO(bdrutu): Do we need to verify that version is hex and that
    // for the version the length is the expected one?
    final boolean isValid =
        traceparent.charAt(TRACE_OPTION_OFFSET - 1) == TRACEPARENT_DELIMITER
            && (traceparent.length() == TRACEPARENT_HEADER_SIZE
                || (traceparent.length() > TRACEPARENT_HEADER_SIZE
                    && traceparent.charAt(TRACEPARENT_HEADER_SIZE) == TRACEPARENT_DELIMITER))
            && traceparent.charAt(SPAN_ID_OFFSET - 1) == TRACEPARENT_DELIMITER
            && traceparent.charAt(TRACE_OPTION_OFFSET - 1) == TRACEPARENT_DELIMITER;
    if (!isValid) {
      logger.info("Unparseable traceparent header. Returning INVALID span context.");
      return SpanContext.getInvalid();
    }

    try {
      final TraceId traceId = TraceId.fromLowerBase16(traceparent, TRACE_ID_OFFSET);
      final SpanId spanId = SpanId.fromLowerBase16(traceparent, SPAN_ID_OFFSET);
      final TraceFlags traceFlags = TraceFlags.fromLowerBase16(traceparent, TRACE_OPTION_OFFSET);
      return SpanContext.createFromRemoteParent(traceId, spanId, traceFlags, TRACE_STATE_DEFAULT);
    } catch (final IllegalArgumentException e) {
      logger.info("Unparseable traceparent header. Returning INVALID span context.");
      return SpanContext.getInvalid();
    }
  }

  private static TraceState extractTraceState(final String traceStateHeader) {
    final TraceState.Builder traceStateBuilder = TraceState.builder();
    final String[] listMembers = TRACESTATE_ENTRY_DELIMITER_SPLIT_PATTERN.split(traceStateHeader);
    checkArgument(
        listMembers.length <= TRACESTATE_MAX_MEMBERS, "TraceState has too many elements.");
    // Iterate in reverse order because when call builder set the elements is added in the
    // front of the list.
    for (int i = listMembers.length - 1; i >= 0; i--) {
      final String listMember = listMembers[i];
      final int index = listMember.indexOf(TRACESTATE_KEY_VALUE_DELIMITER);
      checkArgument(index != -1, "Invalid TraceState list-member format.");
      traceStateBuilder.set(listMember.substring(0, index), listMember.substring(index + 1));
    }
    return traceStateBuilder.build();
  }
}
