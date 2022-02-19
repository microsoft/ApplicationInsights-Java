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
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LogExporterWithAttributeProcessorTest {

  private MockLogExporter mockLogExporter;
  private ProcessorConfig config;

  @BeforeEach
  public void setup() {
    mockLogExporter = new MockLogExporter(Mockito.mock(TelemetryClient.class));
    config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
  }

  @Test
  void noActionTest() {
    config.id = "noAction";

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLogExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithNoValueInActionTest() {
    config.id = "inValidConfigTestWithNoValueInAction";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLogExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithInvalidIncludeTest() {
    config.id = "inValidConfigTestWithInvalidInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLogExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void actionDeleteTest() {
    config.id = "actionDelete";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.DELETE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .put("id", "1234")
            .build();

    MockLogData mockLog = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);

    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("one"))).isNotNull();
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("TESTKEY"))).isNotNull();
  }

  @Test
  void actionInsertTest() {
    config.id = "SimpleRenameLogWithSeparator";
    config.id = "actionInsert";
    ProcessorAction action =
        new ProcessorAction(
            "testNewKey", ProcessorActionType.INSERT, "testNewValue", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();

    MockLogData mockLog = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue");
  }

  @Test
  void actionInsertAndUpdateTest() {
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
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();

    MockLogData mockLog = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testNewValue2");
  }

  @Test
  void actionInsertAndUpdateSameAttributeTest() {
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
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();
    MockLogData log = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue2");
  }

  @Test
  void actionInsertWithDuplicateTest() {
    config.id = "actionInsertWithDuplicate";
    ProcessorAction action =
        new ProcessorAction(
            "testNewKey", ProcessorActionType.INSERT, "testNewValue", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();
    MockLogData log = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testNewValue");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("one"))).isEqualTo("1");
  }

  @Test
  void actionInsertFromAttributeTest() {
    config.id = "actionInsertFromAttribute";
    ProcessorAction action =
        new ProcessorAction("testKey3", ProcessorActionType.INSERT, null, "testKey2", null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .put("testKey2", "testValue2")
            .build();
    MockLogData log = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey3")))
        .isEqualTo("testValue2");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("one"))).isEqualTo("1");
  }

  @Test
  void actionSimpleUpdateLogAndSpanTest() {
    config.id = "actionSimpleUpdateLogAndSpan";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter logExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    // set up log
    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();
    MockLogData log = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    // set up span
    Span span =
        OpenTelemetrySdk.builder()
            .build()
            .getTracer("test")
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    // export log
    List<LogData> logs = new ArrayList<>();
    logs.add(log);
    logExporter.export(logs);

    // export span
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    SpanExporter spanExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);
    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) span).toSpanData());
    spanExporter.export(spans);

    // retrieve updated log
    List<LogData> resultLogs = mockLogExporter.getLogs();
    LogData resultLog = resultLogs.get(0);

    // retrieve updated span
    List<SpanData> resultSpans = mockSpanExporter.getSpans();
    SpanData resultSpan = resultSpans.get(0);

    // verify that resulting logs are filtered in the way we want
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void actionUpdateFromAttributeUpdateTest() {
    config.id = "actionUpdateFromAttributeUpdate";
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, "testKey2", null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData log = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue2");
  }

  @Test
  void complexActionTest() {
    config.id = "complexAction";
    ProcessorAction updateAction =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    ProcessorAction deleteAction =
        new ProcessorAction("testKey2", ProcessorActionType.DELETE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(updateAction);
    actions.add(deleteAction);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData log = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey2"))).isNull();
  }

  @Test
  void simpleIncludeTest() {
    config.id = "simpleInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("svcD").setAttributes(attributesD).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleIncludeWithLogNameTest() {
    config.id = "simpleIncludeWithLogName";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValueC")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValueD")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("svcD").setAttributes(attributesD).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValueC");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValueD");
  }

  @Test
  void simpleIncludeRegexTest() {
    config.id = "simpleIncludeRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.logNames = asList("svc.*", "test.*");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValueC")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("serviceC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValueD")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("serviceD").setAttributes(attributesD).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValueC");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValueD");
  }

  @Test
  void invalidRegexTest() {
    config.id = "invalidRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.logNames = Collections.singletonList("***");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLogExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleIncludeRegexValueTest() {
    config.id = "simpleIncludeRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.logNames = asList("svc.*", "test.*");
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
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValueC")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("serviceC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValueD")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("serviceD").setAttributes(attributesD).build();

    Attributes attributesE =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testV1")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logE = MockLogData.builder().setName("svcE").setAttributes(attributesE).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);
    logs.add(logE);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);
    LogData result5 = result.get(4);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValueC");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValueD");
    assertThat(result5.getAttributes().get(AttributeKey.stringKey("testKey"))).isEqualTo("testV1");
  }

  @Test
  void simpleIncludeHashTest() {
    config.id = "simpleIncludeHash";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logNames = asList("svcA", "svcB", "svcC");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.HASH, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "123")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("serviceC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValueD")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("serviceD").setAttributes(attributesD).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey"))).isEqualTo("123");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValueD");
  }

  @Test
  void simpleExcludeTest() {
    config.id = "simpleIncludeHash";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.logNames = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("svcD").setAttributes(attributesD).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void multiExcludeTest() {
    config.id = "multiExclude";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.logNames = asList("svcA", "svcB");
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
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey3", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("serviceC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("serviceD").setAttributes(attributesD).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
  }

  @Test
  void selectiveProcessingTest() {
    config.id = "selectiveProcessing";
    config.include = new ProcessorIncludeExclude();
    config.exclude = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logNames = asList("svcA", "svcB");
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
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logA = MockLogData.builder().setName("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue1")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logB = MockLogData.builder().setName("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logC = MockLogData.builder().setName("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    MockLogData logD = MockLogData.builder().setName("svcD").setAttributes(attributesD).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData result1 = result.get(0);
    LogData result2 = result.get(1);
    LogData result3 = result.get(2);
    LogData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey2"))).isNull();
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
  }

  @Test
  void actionInsertWithMaskTest() {
    config.id = "actionInsertWithMask";
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
    LogExporter exampleExporter = new LogExporterWithAttributeProcessor(config, mockLogExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "http://example.com/cardid/1234562222227899")
            .put("testKey2", "http://example.com/cardid/1234562222227899")
            .put("testKey3", "http://example.com/cardid/1234562222227899")
            .put("TESTKEY2", "testValue2")
            .put("testKey4", "/TelemetryProcessors/test")
            .put("testKey5", "/abc/xyz")
            .build();
    MockLogData log = MockLogData.builder().setName("my log").setAttributes(attributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockLogExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("http://example.com/cardid/****7899");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("http://example.com/cardid/123456****7899");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey3")))
        .isEqualTo("123456****123456");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("TESTKEY2")))
        .isEqualTo("testValue2");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey4")))
        .isEqualTo("*/TelemetryProcessors/test*");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey5")))
        .isEqualTo("/**abc**/**xyz**");
  }
}
