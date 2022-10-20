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
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.logs.TestLogRecordData;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogExporterWithAttributeProcessorTest {

  private MockLoggerExporter mockLoggerExporter;
  private ProcessorConfig config;

  @BeforeEach
  public void setup() {
    mockLoggerExporter = new MockLoggerExporter();
    config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
  }

  @Test
  void noActionTest() {
    config.id = "noAction";

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLoggerExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithNoValueInActionTest() {
    config.id = "inValidConfigTestWithNoValueInAction";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logBodies = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLoggerExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithInvalidIncludeTest() {
    config.id = "inValidConfigTestWithInvalidInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logBodies = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLoggerExporter))
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .put("id", "1234")
            .build();

    TestLogRecordData mockLog =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);

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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();

    TestLogRecordData mockLog =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();

    TestLogRecordData mockLog =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();
    TestLogRecordData log =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();
    TestLogRecordData log =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData log =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
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
    LogRecordExporter logExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    // set up log
    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("TESTKEY", "testValue2")
            .build();
    TestLogRecordData log =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

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
    List<LogRecordData> logs = new ArrayList<>();
    logs.add(log);
    logExporter.export(logs);

    // export span
    MockSpanExporter mockSpanExporter = new MockSpanExporter();
    SpanExporter spanExporter = new SpanExporterWithAttributeProcessor(config, mockSpanExporter);
    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) span).toSpanData());
    spanExporter.export(spans);

    // retrieve updated log
    List<LogRecordData> resultLogs = mockLoggerExporter.getLogs();
    LogRecordData resultLog = resultLogs.get(0);

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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData log =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData log =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey2"))).isNull();
  }

  @Test
  void simpleIncludeTest() {
    config.id = "simpleInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logBodies = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("svcD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

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
    config.include.logBodies = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValueC")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValueD")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("svcD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

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
    config.include.logBodies = asList("svc.*", "test.*");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValueC")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("serviceC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValueD")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("serviceD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

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
    config.include.logBodies = Collections.singletonList("***");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new LogExporterWithAttributeProcessor(config, mockLoggerExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleIncludeRegexValueTest() {
    config.id = "simpleIncludeRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.logBodies = asList("svc.*", "test.*");
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValueC")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("serviceC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValueD")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("serviceD").setAttributes(attributesD).build();

    Attributes attributesE =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testV1")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logE =
        TestLogRecordData.builder().setBody("svcE").setAttributes(attributesE).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);
    logs.add(logE);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);
    LogRecordData result5 = result.get(4);

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
    config.include.logBodies = asList("svcA", "svcB", "svcC");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.HASH, null, null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("svcD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleExcludeTest() {
    config.id = "simpleExclude";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.logBodies = asList("svcA", "svcB");
    ProcessorAction action =
        new ProcessorAction("testKey", ProcessorActionType.UPDATE, "redacted", null, null, null);
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("svcD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

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
  void multiIncludeTest() {
    config.id = "multiInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.logBodies = asList("svcA", "svcB");
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey3", "testValue3")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("svcD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

    assertThat(result1.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(result2.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(result3.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(result4.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void multiExcludeTest() {
    config.id = "multiExclude";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.logBodies = asList("svcA", "svcB");
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue")
            .put("testKey3", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("serviceC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("serviceD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

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
    config.include.logBodies = asList("svcA", "svcB");
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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

    Attributes attributesA =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logA =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributesA).build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("testKey", "testValue1")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logB =
        TestLogRecordData.builder().setBody("svcB").setAttributes(attributesB).build();

    Attributes attributesC =
        Attributes.builder()
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logC =
        TestLogRecordData.builder().setBody("svcC").setAttributes(attributesC).build();

    Attributes attributesD =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("testKey", "testValue")
            .put("testKey2", "testValue2")
            .build();
    TestLogRecordData logD =
        TestLogRecordData.builder().setBody("svcD").setAttributes(attributesD).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(logA);
    logs.add(logB);
    logs.add(logC);
    logs.add(logD);

    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData result1 = result.get(0);
    LogRecordData result2 = result.get(1);
    LogRecordData result3 = result.get(2);
    LogRecordData result4 = result.get(3);

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
    LogRecordExporter exampleExporter =
        new LogExporterWithAttributeProcessor(config, mockLoggerExporter);

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
    TestLogRecordData log =
        TestLogRecordData.builder().setBody("my log").setAttributes(attributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(log);
    exampleExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockLoggerExporter.getLogs();
    LogRecordData resultLog = result.get(0);
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
