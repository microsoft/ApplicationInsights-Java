package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span.Kind;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;

public class MySpanData implements SpanData {

  private final SpanData delegate;
  private final Attributes attributes;
  private final String spanName;

  public MySpanData(SpanData delegate, Attributes attributes) {
    this(delegate, attributes, delegate.getName());
  }

  public MySpanData(SpanData delegate, Attributes attributes, String spanName) {
    this.delegate = delegate;
    this.attributes = attributes;
    this.spanName = spanName;
  }

  @Override
  public String getTraceId() {
    return delegate.getTraceId();
  }

  @Override
  public String getSpanId() {
    return delegate.getSpanId();
  }

  @Override
  public boolean isSampled() {
    return delegate.isSampled();
  }

  @Override
  public TraceState getTraceState() {
    return delegate.getTraceState();
  }

  @Override
  public SpanContext getParentSpanContext() {
    return delegate.getParentSpanContext();
  }

  @Override
  public String getParentSpanId() {
    return delegate.getParentSpanId();
  }

  @Override
  public Resource getResource() {
    return delegate.getResource();
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return delegate.getInstrumentationLibraryInfo();
  }

  @Override
  public String getName() {
    return spanName;
  }

  @Override
  public Kind getKind() {
    return delegate.getKind();
  }

  @Override
  public long getStartEpochNanos() {
    return delegate.getStartEpochNanos();
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public List<Event> getEvents() {
    return delegate.getEvents();
  }

  @Override
  public List<Link> getLinks() {
    return delegate.getLinks();
  }

  @Override
  public Status getStatus() {
    return delegate.getStatus();
  }

  @Override
  public long getEndEpochNanos() {
    return delegate.getEndEpochNanos();
  }

  @Override
  public boolean hasRemoteParent() {
    return delegate.hasRemoteParent();
  }

  @Override
  public boolean hasEnded() {
    return delegate.hasEnded();
  }

  @Override
  public int getTotalRecordedEvents() {
    return delegate.getTotalRecordedEvents();
  }

  @Override
  public int getTotalRecordedLinks() {
    return delegate.getTotalRecordedLinks();
  }

  @Override
  public int getTotalAttributeCount() {
    return delegate.getTotalAttributeCount();
  }
}
