// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public class TraceAssert {

  private final List<Envelope> requests;
  private final List<Envelope> dependencies;
  private final List<Envelope> messages;
  private final List<Envelope> exceptions;

  public TraceAssert(List<Envelope> trace) {

    // TODO sort by parent/child if same timestamp?
    List<Envelope> sorted =
        trace.stream()
            .sorted(comparing(envelope -> Instant.parse(envelope.getTime())))
            .collect(toList());

    requests =
        sorted.stream()
            .filter(envelope -> envelope.getData().getBaseType().equals("RequestData"))
            .collect(toList());
    dependencies =
        sorted.stream()
            .filter(envelope -> envelope.getData().getBaseType().equals("RemoteDependencyData"))
            .collect(toList());
    messages =
        sorted.stream()
            .filter(envelope -> envelope.getData().getBaseType().equals("MessageData"))
            .collect(toList());
    exceptions =
        sorted.stream()
            .filter(envelope -> envelope.getData().getBaseType().equals("ExceptionData"))
            .collect(toList());
  }

  @CanIgnoreReturnValue
  public TraceAssert hasRequestSatisying(Consumer<RequestAssert> assertion) {
    assertThat(requests).anySatisfy(envelope -> assertion.accept(new RequestAssert(envelope)));
    return this;
  }

  @CanIgnoreReturnValue
  public TraceAssert hasDependencySatisying(Consumer<DependencyAssert> assertion) {
    assertThat(dependencies)
        .anySatisfy(envelope -> assertion.accept(new DependencyAssert(envelope)));
    return this;
  }

  @CanIgnoreReturnValue
  public TraceAssert hasMessageCount(int count) {
    assertThat(messages).hasSize(count);
    return this;
  }

  @CanIgnoreReturnValue
  public TraceAssert hasMessageSatisfying(Consumer<MessageAssert> assertion) {
    assertThat(messages).anySatisfy(envelope -> assertion.accept(new MessageAssert(envelope)));
    return this;
  }

  @CanIgnoreReturnValue
  public TraceAssert hasExceptionSatisfying(Consumer<ExceptionAssert> assertion) {
    assertThat(exceptions).anySatisfy(envelope -> assertion.accept(new ExceptionAssert(envelope)));
    return this;
  }

  public List<Envelope> getMessages() {
    return messages;
  }

  public List<Envelope> getExceptions() {
    return exceptions;
  }

  public String getRequestId(int index) {
    Data<?> data = (Data<?>) requests.get(index).getData();
    return ((RequestData) data.getBaseData()).getId();
  }

  public String getDependencyId(int index) {
    Data<?> data = (Data<?>) dependencies.get(index).getData();
    return ((RemoteDependencyData) data.getBaseData()).getId();
  }
}
