// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static java.util.concurrent.TimeUnit.SECONDS;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import javax.annotation.Nullable;

// this is just needed temporarily until
// https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8761
class TimestampingLogRecordProcessor implements LogRecordProcessor {

  private final LogRecordProcessor delegate;

  public TimestampingLogRecordProcessor(LogRecordProcessor delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    delegate.onEmit(context, new TimestampingReadWriteLogRecord(logRecord, Instant.now()));
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return delegate.forceFlush();
  }

  @Override
  public void close() {
    delegate.close();
  }

  private static class TimestampingReadWriteLogRecord implements ReadWriteLogRecord {

    private final ReadWriteLogRecord delegate;
    private final Instant timestamp;

    private TimestampingReadWriteLogRecord(ReadWriteLogRecord delegate, Instant timestamp) {
      this.delegate = delegate;
      this.timestamp = timestamp;
    }

    @Override
    public <T> ReadWriteLogRecord setAttribute(AttributeKey<T> key, T value) {
      return delegate.setAttribute(key, value);
    }

    @Override
    public LogRecordData toLogRecordData() {
      return new TimestampedLogRecordData(delegate.toLogRecordData(), timestamp);
    }
  }

  private static class TimestampedLogRecordData implements LogRecordData {

    private final LogRecordData delegate;
    private final Instant timestamp;

    private TimestampedLogRecordData(LogRecordData delegate, Instant timestamp) {
      this.delegate = delegate;
      this.timestamp = timestamp;
    }

    @Override
    public Resource getResource() {
      return delegate.getResource();
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
      return delegate.getInstrumentationScopeInfo();
    }

    @Override
    public long getTimestampEpochNanos() {
      long timestampEpochNanos = delegate.getTimestampEpochNanos();
      if (timestampEpochNanos == 0) {
        return SECONDS.toNanos(timestamp.getEpochSecond()) + timestamp.getNano();
      }
      return timestampEpochNanos;
    }

    @Override
    public long getObservedTimestampEpochNanos() {
      return delegate.getObservedTimestampEpochNanos();
    }

    @Override
    public SpanContext getSpanContext() {
      return delegate.getSpanContext();
    }

    @Override
    public Severity getSeverity() {
      return delegate.getSeverity();
    }

    @Override
    @Nullable
    public String getSeverityText() {
      return delegate.getSeverityText();
    }

    @Override
    public Body getBody() {
      return delegate.getBody();
    }

    @Override
    public Attributes getAttributes() {
      return delegate.getAttributes();
    }

    @Override
    public int getTotalAttributeCount() {
      return delegate.getTotalAttributeCount();
    }

    @Override
    public String toString() {
      return "SdkLogRecordData{resource="
          + this.getResource()
          + ", instrumentationScopeInfo="
          + this.getInstrumentationScopeInfo()
          + ", timestampEpochNanos="
          + this.getTimestampEpochNanos()
          + ", observedTimestampEpochNanos="
          + this.getObservedTimestampEpochNanos()
          + ", spanContext="
          + this.getSpanContext()
          + ", severity="
          + this.getSeverity()
          + ", severityText="
          + this.getSeverityText()
          + ", body="
          + this.getBody()
          + ", attributes="
          + this.getAttributes()
          + ", totalAttributeCount="
          + this.getTotalAttributeCount()
          + "}";
    }
  }
}
