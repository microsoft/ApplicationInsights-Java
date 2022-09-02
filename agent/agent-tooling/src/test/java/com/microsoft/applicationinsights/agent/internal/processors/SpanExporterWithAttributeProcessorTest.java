// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorType;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpanExporterWithAttributeProcessorTest {

  private final Tracer tracer = OpenTelemetrySdk.builder().build().getTracer("test");

  @Test
  void noActionTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "noAction";

    assertThatThrownBy(() -> new SpanExporterWithAttributeProcessor(config, mockSpanExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithNoValueInActionTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "inValidConfigTestWithNoValueInAction";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new SpanExporterWithAttributeProcessor(config, mockSpanExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithInvalidIncludeTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "inValidConfigTestWithInvalidInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new SpanExporterWithAttributeProcessor(config, mockSpanExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void actionDeleteTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionDelete";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.DELETE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("TESTKEY"))).isNotNull();
  }

  @Test
  void actionInsertTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsert";
    ProcessorAction action =
        new ProcessorAction(
            "testNewKey", ProcessorActionType.INSERT, "testNewValue", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue");
  }

  @Test
  void actionInsertAndUpdateTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertAndUpdate";
    ProcessorAction action =
        new ProcessorAction(
            "testNewKey", ProcessorActionType.INSERT, "testNewValue", null, null, null);
    ProcessorAction updateAction =
        new ProcessorAction(
            "testKey", ProcessorActionType.UPDATE, "testNewValue2", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    actions.add(updateAction);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testNewValue2");
  }

  @Test
  void actionInsertAndUpdateSameAttributeTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertAndUpdate";
    ProcessorAction action =
        new ProcessorAction(
            "testNewKey", ProcessorActionType.INSERT, "testNewValue", null, null, null);
    ProcessorAction updateAction =
        new ProcessorAction(
            "testNewKey", ProcessorActionType.UPDATE, "testNewValue2", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    actions.add(updateAction);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue2");
  }

  @Test
  void actionInsertWithDuplicateTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertWithDuplicate";
    ProcessorAction action =
        new ProcessorAction(
            "testNewKey", ProcessorActionType.INSERT, "testNewValue", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testNewValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))).isEqualTo("1");
  }

  @Test
  void actionInsertFromAttributeTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertFromAttribute";
    ProcessorAction action =
        new ProcessorAction("testKey3", ProcessorActionType.INSERT, null, "testKey2", null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey3")))
        .isEqualTo("testValue2");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))).isEqualTo("1");
  }

  @Test
  void actionSimpleUpdateTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionSimpleUpdate";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    Span log =
        tracer
            .spanBuilder("my log")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .setAttribute("applicationinsights.internal.log", true)
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();
    SpanData logData = ((ReadableSpan) log).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    spans.add(logData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);
    SpanData resultLog = result.get(1);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void actionUpdateFromAttributeUpdateTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionUpdateFromAttributeUpdate";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, "testKey2", null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue2");
  }

  @Test
  void complexActionTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "complexAction";
    ProcessorAction updateAction =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    ProcessorAction deleteAction =
        new ProcessorAction("testKey2", ProcessorActionType.DELETE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(updateAction);
    actions.add(deleteAction);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey2"))).isNull();
  }

  @Test
  void simpleIncludeTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleIncludeWithSpanNamesTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeWithSpanNames";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span logA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .setAttribute("applicationinsights.internal.log", true)
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());
    spans.add(((ReadableSpan) logA).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultLogA = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    // Make sure log is not updated, since we have spanNames in include criteria
    assertThat(resultLogA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleIncludeRegexTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = asList("svc.*", "test.*");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void invalidRegexTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "invalidRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = Collections.singletonList("***");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new SpanExporterWithAttributeProcessor(config, mockSpanExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleIncludeRegexValueTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeRegexValue";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = asList("svc.*", "test.*");
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "Value.*";
    config.include.attributes = new ArrayList<>();
    config.include.attributes.add(attributeWithValue);
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue2")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanE =
        tracer
            .spanBuilder("svcE")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testV1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());
    spans.add(((ReadableSpan) spanE).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);
    SpanData resultSpanE = result.get(4);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanE.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testV1");
  }

  @Test
  void simpleIncludeRegexNoValueTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeRegexNoValue";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = asList("svc.*", "test.*");
    ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
    attributeWithNoValue.key = "testKey";
    config.include.attributes = new ArrayList<>();
    config.include.attributes.add(attributeWithNoValue);
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue2")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanE =
        tracer
            .spanBuilder("svcE")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testV1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());
    spans.add(((ReadableSpan) spanE).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);
    SpanData resultSpanE = result.get(4);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanE.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void simpleIncludeHashTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeHash";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = asList("svcA", "svcB", "svcC");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.HASH, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", 2L)
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleExcludeTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleExclude";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.spanNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void simpleExcludeRegexTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleExcludeRegex";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.REGEXP;
    config.exclude.spanNames = Collections.singletonList("svc.*");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void multiIncludeTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "multiInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = asList("svcA", "svcB");
    config.include.attributes = new ArrayList<>();
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "testValue";
    ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
    attributeWithNoValue.key = "testKey2";
    config.include.attributes.add(attributeWithValue);
    config.include.attributes.add(attributeWithNoValue);
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.DELETE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey3", "testValue3")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void multiExcludeTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "multiExclude";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.spanNames = asList("svcA", "svcB");
    config.exclude.attributes = new ArrayList<>();
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "testValue";
    config.exclude.attributes.add(attributeWithValue);
    ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
    attributeWithNoValue.key = "testKey2";
    config.exclude.attributes.add(attributeWithNoValue);
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.DELETE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey3", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
  }

  @Test
  void selectiveProcessingTest() { // With both include and exclude
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "selectiveProcessing";
    config.include = new ProcessorIncludeExclude();
    config.exclude = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = asList("svcA", "svcB");
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.attributes = new ArrayList<>();
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "testValue";
    config.exclude.attributes.add(attributeWithValue);
    ProcessorAction action =
        new ProcessorAction("testKey2", ProcessorActionType.DELETE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey2"))).isNull();
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
  }

  @Test
  void actionInsertWithExtractTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionExtract";
    String regex =
        "^(?<httpProtocol>.*)://(?<httpDomain>.*)/(?<httpPath>.*)([?&])(?<httpQueryParams>.*)";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.EXTRACT, null, null, regex, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute(
                "testKey", "http://example.com/path?queryParam1=value1,queryParam2=value2")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpProtocol")))
        .isEqualTo("http");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpDomain")))
        .isEqualTo("example.com");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath")))
        .isEqualTo("path");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpQueryParams")))
        .isEqualTo("queryParam1=value1,queryParam2=value2");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("http://example.com/path?queryParam1=value1,queryParam2=value2");
  }

  @Test
  void actionInsertWithExtractDuplicateTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionExtract";
    String regex =
        "^(?<httpProtocol>.*)://(?<httpDomain>.*)/(?<httpPath>.*)([?&])(?<httpQueryParams>.*)";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.EXTRACT, null, null, regex, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute(
                "testKey", "http://example.com/path?queryParam1=value1,queryParam2=value2")
            .setAttribute("httpPath", "oldPath")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpProtocol")))
        .isEqualTo("http");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpDomain")))
        .isEqualTo("example.com");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath")))
        .isEqualTo("path");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath")))
        .isNotEqualTo("oldPath");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpQueryParams")))
        .isEqualTo("queryParam1=value1,queryParam2=value2");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("http://example.com/path?queryParam1=value1,queryParam2=value2");
  }

  @Test
  void actionInsertWithMaskTest() {
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionMask";
    String regex =
        "^(?<uriNoCard>.*\\/cardid\\/)(?<cardStart>[0-9]{6})[0-9]{6}(?<cardEnd>[0-9]{4,6}).*";
    String regex2 = "(?<httpPath>.+)";
    String regex3 = "(?<httpPath>[a-zA-Z]+)";
    ProcessorAction action =
        new ProcessorAction(
            "testKey", ProcessorActionType.MASK, null, null, regex, "${uriNoCard}****${cardEnd}");
    ProcessorAction action2 =
        new ProcessorAction(
            "testKey2",
            ProcessorActionType.MASK,
            null,
            null,
            regex,
            "${uriNoCard}${cardStart}****${cardEnd}");
    ProcessorAction action3 =
        new ProcessorAction(
            "testKey3",
            ProcessorActionType.MASK,
            null,
            null,
            regex,
            "${cardStart}****${cardStart}");
    ProcessorAction action4 =
        new ProcessorAction(
            "testKey4", ProcessorActionType.MASK, null, null, regex2, "*${httpPath}*");
    ProcessorAction action5 =
        new ProcessorAction(
            "testKey5", ProcessorActionType.MASK, null, null, regex3, "**${httpPath}**");
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    actions.add(action2);
    actions.add(action3);
    actions.add(action4);
    actions.add(action5);
    config.actions = actions;
    SpanExporter exampleExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "http://example.com/cardid/1234562222227899")
            .setAttribute("testKey2", "http://example.com/cardid/1234562222227899")
            .setAttribute("testKey3", "http://example.com/cardid/1234562222227899")
            .setAttribute("TESTKEY2", "testValue2")
            .setAttribute("testKey4", "/TelemetryProcessors/test")
            .setAttribute("testKey5", "/abc/xyz")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockSpanExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("http://example.com/cardid/****7899");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("http://example.com/cardid/123456****7899");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey3")))
        .isEqualTo("123456****123456");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("TESTKEY2")))
        .isEqualTo("testValue2");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey4")))
        .isEqualTo("*/TelemetryProcessors/test*");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey5")))
        .isEqualTo("/**abc**/**xyz**");
  }
}
