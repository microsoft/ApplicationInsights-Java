package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.List;

import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;

public class MySpanData implements SpanData {

  private final SpanData delegate;
  private final ReadableAttributes attributes;
  private final String spanName;

  public MySpanData(SpanData delegate, ReadableAttributes attributes) {
    this(delegate, attributes, delegate.getName());
  }

  public MySpanData(SpanData delegate, ReadableAttributes attributes, String spanName) {
    this.delegate = delegate;
    this.attributes = attributes;
    this.spanName = spanName;
  }

  @Override public TraceId getTraceId() {
    return delegate.getTraceId();
  }

  @Override public SpanId getSpanId() {
    return delegate.getSpanId();
  }

  @Override public TraceFlags getTraceFlags() {
    return delegate.getTraceFlags();
  }

  @Override public TraceState getTraceState() {
    return delegate.getTraceState();
  }

  @Override public SpanId getParentSpanId() {
    return delegate.getParentSpanId();
  }

  @Override public Resource getResource() {
    return delegate.getResource();
  }

  @Override public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return delegate.getInstrumentationLibraryInfo();
  }

  @Override public String getName() {
    return spanName;
    //return delegate.getName();
  }

  @Override public Kind getKind() {
    return delegate.getKind();
  }

  @Override public long getStartEpochNanos() {
    return delegate.getStartEpochNanos();
  }

  @Override public ReadableAttributes getAttributes() {
    return attributes;
  }

  @Override public List<Event> getEvents() {
    return delegate.getEvents();
  }

  @Override public List<Link> getLinks() {
    return delegate.getLinks();
  }

  @Override public Status getStatus() {
    return delegate.getStatus();
  }

  @Override public long getEndEpochNanos() {
    return delegate.getEndEpochNanos();
  }

  @Override public boolean getHasRemoteParent() {
    return delegate.getHasRemoteParent();
  }

  @Override public boolean getHasEnded() {
    return delegate.getHasEnded();
  }

  @Override public int getTotalRecordedEvents() {
    return delegate.getTotalRecordedEvents();
  }

  @Override public int getTotalRecordedLinks() {
    return delegate.getTotalRecordedLinks();
  }

  @Override public int getTotalAttributeCount() {
    return delegate.getTotalAttributeCount();
  }
}
