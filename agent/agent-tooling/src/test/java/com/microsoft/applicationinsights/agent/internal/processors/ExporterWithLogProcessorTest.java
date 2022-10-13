// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.NameConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ToAttributeConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.logs.TestLogRecordData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExporterWithLogProcessorTest {

  private MockLoggerExporter mockExporter;
  private ProcessorConfig config;
  private Attributes attributes;

  @BeforeEach
  public void setup() {
    mockExporter = new MockLoggerExporter();
    config = new ProcessorConfig();
    config.type = ProcessorType.LOG;
    attributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("db.svc", "location")
            .put("operation", "get")
            .put("id", "1234")
            .build();
  }

  @Test
  void noBodyObjectTest() {
    config.id = "noBodyObjectTest";

    assertThatThrownBy(() -> new ExporterWithLogProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithNoFromOrToAttributesTest() {
    config.id = "inValidConfigTestWithToAttributesNoRules";
    config.body = new NameConfig();
    assertThatThrownBy(() -> new ExporterWithLogProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithToAttributesNoRulesTest() {
    config.id = "inValidConfigTestWithToAttributesNoRules";
    config.body = new NameConfig();
    config.body.toAttributes = new ToAttributeConfig();

    assertThatThrownBy(() -> new ExporterWithLogProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleRenameLogMessageTest() {
    config.id = "SimpleRenameLogMessage";
    config.body = new NameConfig();
    config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    LogRecordExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    TestLogRecordData mockLog =
        TestLogRecordData.builder().setBody("logA").setAttributes(attributes).build();
    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockExporter.getLogs();
    LogRecordData resultLog = result.get(0);
    assertThat(resultLog.getBody().asString()).isEqualTo("locationget1234");
  }

  @Test
  void simpleRenameLogWithSeparatorTest() {
    config.id = "SimpleRenameLogWithSeparator";
    config.body = new NameConfig();
    config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    config.body.separator = "::";
    LogRecordExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    TestLogRecordData mockLog =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributes).build();
    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockExporter.getLogs();
    LogRecordData resultLog = result.get(0);
    assertThat(resultLog.getBody().asString()).isEqualTo("location::get::1234");
  }

  @Test
  void simpleRenameLogWithMissingKeysTest() {
    config.id = "SimpleRenameLogWithMissingKeys";
    config.body = new NameConfig();
    config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    config.body.separator = "::";
    LogRecordExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);

    TestLogRecordData mockLog =
        TestLogRecordData.builder().setBody("svcA").setAttributes(attributes).build();
    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockExporter.getLogs();
    LogRecordData resultLog = result.get(0);
    assertThat(resultLog.getBody().asString()).isEqualTo("location::get::1234");
  }

  @Test
  void invalidRegexInRulesTest() {
    config.id = "InvalidRegexInRules";
    config.body = new NameConfig();
    ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
    toAttributeConfig.rules = new ArrayList<>();
    toAttributeConfig.rules.add("***");
    config.body.toAttributes = toAttributeConfig;

    assertThatThrownBy(() -> new ExporterWithLogProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleToAttributesTest() {
    config.id = "SimpleToAttributes";
    config.body = new NameConfig();
    ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
    toAttributeConfig.rules = new ArrayList<>();
    toAttributeConfig.rules.add("^/api/v1/document/(?<documentId>.*)/update$");
    config.body.toAttributes = toAttributeConfig;
    LogRecordExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    TestLogRecordData mockLog =
        TestLogRecordData.builder()
            .setBody("/api/v1/document/12345678/update")
            .setAttributes(attributes)
            .build();
    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockExporter.getLogs();
    LogRecordData resultLog = result.get(0);
    assertThat(
            Objects.requireNonNull(
                resultLog.getAttributes().get(AttributeKey.stringKey("documentId"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultLog.getAttributes().get(AttributeKey.stringKey("documentId"))))
        .isEqualTo("12345678");
    assertThat(resultLog.getBody().asString()).isEqualTo("/api/v1/document/{documentId}/update");
  }

  @Test
  void multiRuleToAttributesTest() {
    config.id = "MultiRuleToAttributes";
    config.body = new NameConfig();
    ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
    toAttributeConfig.rules = new ArrayList<>();
    toAttributeConfig.rules.add("Password=(?<password1>[^ ]+)");
    toAttributeConfig.rules.add("Pass=(?<password2>[^ ]+)");
    config.body.toAttributes = toAttributeConfig;
    LogRecordExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    TestLogRecordData mockLogA =
        TestLogRecordData.builder()
            .setBody("yyyPassword=123 aba Pass=555 xyx Pass=777 zzz")
            .setAttributes(attributes)
            .build();

    Attributes attributesB =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("db.svc", "location")
            .put("operation", "get")
            .put("id", "1234")
            .put("password", "234")
            .build();
    TestLogRecordData mockLogB =
        TestLogRecordData.builder()
            .setBody("yyyPassword=**** aba")
            .setAttributes(attributesB)
            .build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLogA);
    logs.add(mockLogB);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogRecordData> result = mockExporter.getLogs();
    LogRecordData resultA = result.get(0);
    LogRecordData resultB = result.get(1);
    assertThat(
            Objects.requireNonNull(
                resultA.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultA.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isEqualTo("123");
    assertThat(
            Objects.requireNonNull(
                resultA.getAttributes().get(AttributeKey.stringKey("password2"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultA.getAttributes().get(AttributeKey.stringKey("password2"))))
        .isEqualTo("555");
    assertThat(resultA.getBody().asString())
        .isEqualTo("yyyPassword={password1} aba Pass={password2} xyx Pass=777 zzz");
    assertThat(
            Objects.requireNonNull(
                resultB.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultB.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isEqualTo("****");
    assertThat(resultB.getBody().asString()).isEqualTo("yyyPassword={password1} aba");
  }

  @Test
  void simpleRenameLogTestWithLogProcessor() {
    config.id = "SimpleRenameLog";
    config.body = new NameConfig();
    config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    LogRecordExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);

    Attributes newAttributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("db.svc", "location")
            .put("operation", "get")
            .put("id", "1234")
            .build();
    TestLogRecordData mockLog =
        TestLogRecordData.builder().setBody("svcA").setAttributes(newAttributes).build();

    List<LogRecordData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are not modified
    List<LogRecordData> result = mockExporter.getLogs();
    LogRecordData resultLog = result.get(0);
    assertThat(resultLog.getBody().asString()).isEqualTo("locationget1234");
  }
}
