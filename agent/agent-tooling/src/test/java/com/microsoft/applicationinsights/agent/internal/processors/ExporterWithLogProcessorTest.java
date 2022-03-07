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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.NameConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ToAttributeConfig;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.export.LogExporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExporterWithLogProcessorTest {

  private MockLogExporter mockExporter;
  private ProcessorConfig config;
  private Attributes attributes;

  @BeforeEach
  public void setup() {
    mockExporter = new MockLogExporter(Mockito.mock(TelemetryClient.class));
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
    LogExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    MockLogData mockLog = MockLogData.builder().setBody(Body.string("logA")).setAttributes(attributes).build();
    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getBody().asString()).isEqualTo("locationget1234");
  }

  @Test
  void simpleRenameLogWithSeparatorTest() {
    config.id = "SimpleRenameLogWithSeparator";
    config.body = new NameConfig();
    config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    config.body.separator = "::";
    LogExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    MockLogData mockLog = MockLogData.builder().setBody(Body.string("svcA")).setAttributes(attributes).build();
    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getBody().asString()).isEqualTo("location::get::1234");
  }

  @Test
  void simpleRenameLogWithMissingKeysTest() {
    config.id = "SimpleRenameLogWithMissingKeys";
    config.body = new NameConfig();
    config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    config.body.separator = "::";
    LogExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);

    MockLogData mockLog = MockLogData.builder().setBody(Body.string("svcA")).setAttributes(attributes).build();
    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockExporter.getLogs();
    LogData resultLog = result.get(0);
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
    LogExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    MockLogData mockLog =
        MockLogData.builder()
            .setBody(Body.string("/api/v1/document/12345678/update"))
            .setAttributes(attributes)
            .build();
    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockExporter.getLogs();
    LogData resultLog = result.get(0);
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
    LogExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);
    MockLogData mockLogA =
        MockLogData.builder()
            .setBody(Body.string("yyyPassword=123 aba Pass=555 xyx Pass=777 zzz"))
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
    MockLogData mockLogB =
        MockLogData.builder().setBody(Body.string("yyyPassword=**** aba")).setAttributes(attributesB).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(mockLogA);
    logs.add(mockLogB);
    logExporter.export(logs);

    // verify that resulting logs are filtered in the way we want
    List<LogData> result = mockExporter.getLogs();
    LogData resultA = result.get(0);
    LogData resultB = result.get(1);
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
    LogExporter logExporter = new ExporterWithLogProcessor(config, mockExporter);

    Attributes newAttributes =
        Attributes.builder()
            .put("one", "1")
            .put("two", 2L)
            .put("db.svc", "location")
            .put("operation", "get")
            .put("id", "1234")
            .build();
    MockLogData mockLog =
        MockLogData.builder().setBody(Body.string("svcA")).setAttributes(newAttributes).build();

    List<LogData> logs = new ArrayList<>();
    logs.add(mockLog);
    logExporter.export(logs);

    // verify that resulting logs are not modified
    List<LogData> result = mockExporter.getLogs();
    LogData resultLog = result.get(0);
    assertThat(resultLog.getBody().asString()).isEqualTo("locationget1234");
  }
}
