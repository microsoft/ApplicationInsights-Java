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
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.NameConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ToAttributeConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class ExporterWithSpanProcessorTest {

  private final Tracer tracer = OpenTelemetrySdk.builder().build().getTracer("test");

  @Test
  void noNameObjectTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "noNameObjectTest";

    assertThatThrownBy(() -> new ExporterWithSpanProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithNoFromOrToAttributesTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "inValidConfigTestWithToAttributesNoRules";
    config.name = new NameConfig();

    assertThatThrownBy(() -> new ExporterWithSpanProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithToAttributesNoRulesTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "inValidConfigTestWithToAttributesNoRules";
    config.name = new NameConfig();
    config.name.toAttributes = new ToAttributeConfig();

    assertThatThrownBy(() -> new ExporterWithSpanProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleRenameSpanTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "SimpleRenameSpan";
    config.name = new NameConfig();
    config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);
    assertThat(resultSpan.getName()).isEqualTo("locationget1234");
  }

  @Test
  void simpleRenameSpanWithSeparatorTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "SimpleRenameSpanWithSeparator";
    config.name = new NameConfig();
    config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    config.name.separator = "::";
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);
    assertThat(resultSpan.getName()).isEqualTo("location::get::1234");
  }

  @Test
  void simpleRenameSpanWithMissingKeysTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "SimpleRenameSpanWithMissingKeys";
    config.name = new NameConfig();
    config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    config.name.separator = "::";
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);
    assertThat(resultSpan.getName()).isEqualTo("svcA");
  }

  @Test
  void renameSpanWithIncludeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "RenameSpanWithInclude";
    config.name = new NameConfig();
    config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB");
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();

    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
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
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);
    assertThat(resultSpanA.getName()).isEqualTo("locationget1234");
    assertThat(resultSpanB.getName()).isEqualTo("locationget1234");
    assertThat(resultSpanC.getName()).isEqualTo("svcC");
    assertThat(resultSpanD.getName()).isEqualTo("svcD");
  }

  @Test
  void invalidRegexInRulesTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "InvalidRegexInRules";
    config.name = new NameConfig();
    ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
    toAttributeConfig.rules = new ArrayList<>();
    toAttributeConfig.rules.add("***");
    config.name.toAttributes = toAttributeConfig;

    assertThatThrownBy(() -> new ExporterWithSpanProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleToAttributesTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "SimpleToAttributes";
    config.name = new NameConfig();
    ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
    toAttributeConfig.rules = new ArrayList<>();
    toAttributeConfig.rules.add("^/api/v1/document/(?<documentId>.*)/update$");
    config.name.toAttributes = toAttributeConfig;
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("/api/v1/document/12345678/update")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);
    assertThat(
            Objects.requireNonNull(
                resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))))
        .isEqualTo("12345678");
    assertThat(resultSpan.getName()).isEqualTo("/api/v1/document/{documentId}/update");
  }

  @Test
  void multiRuleToAttributesTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "MultiRuleToAttributes";
    config.name = new NameConfig();
    ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
    toAttributeConfig.rules = new ArrayList<>();
    toAttributeConfig.rules.add("Password=(?<password1>[^ ]+)");
    toAttributeConfig.rules.add("Pass=(?<password2>[^ ]+)");
    config.name.toAttributes = toAttributeConfig;
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("yyyPassword=123 aba Pass=555 xyx Pass=777 zzz")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("yyyPassword=**** aba")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .setAttribute("password", "234")
            .startSpan();

    SpanData spanDataA = ((ReadableSpan) spanA).toSpanData();
    SpanData spanDataB = ((ReadableSpan) spanB).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanDataA);
    spans.add(spanDataB);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    assertThat(
            Objects.requireNonNull(
                resultSpanA.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultSpanA.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isEqualTo("123");
    assertThat(
            Objects.requireNonNull(
                resultSpanA.getAttributes().get(AttributeKey.stringKey("password2"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultSpanA.getAttributes().get(AttributeKey.stringKey("password2"))))
        .isEqualTo("555");
    assertThat(resultSpanA.getName())
        .isEqualTo("yyyPassword={password1} aba Pass={password2} xyx Pass=777 zzz");
    assertThat(
            Objects.requireNonNull(
                resultSpanB.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultSpanB.getAttributes().get(AttributeKey.stringKey("password1"))))
        .isEqualTo("****");
    assertThat(resultSpanB.getName()).isEqualTo("yyyPassword={password1} aba");
  }

  @Test
  void extractAttributesWithIncludeExcludeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "ExtractAttributesWithIncludeExclude";
    config.name = new NameConfig();
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = Arrays.asList("^(.*?)/(.*?)$");
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.spanNames = Arrays.asList("donot/change");
    config.name.toAttributes = new ToAttributeConfig();
    config.name.toAttributes.rules = Arrays.asList("(?<operationwebsite>.*?)$");
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA/test")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();

    Span spanB =
        tracer
            .spanBuilder("svcB/test")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("donot/change")
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
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);
    assertThat(resultSpanA.getName()).isEqualTo("{operationwebsite}");
    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("operationwebsite")))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultSpanA.getAttributes().get(AttributeKey.stringKey("operationwebsite"))))
        .isEqualTo("svcA/test");
    assertThat(resultSpanB.getName()).isEqualTo("{operationwebsite}");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("operationwebsite")))
        .isNotNull();
    assertThat(
            Objects.requireNonNull(
                resultSpanB.getAttributes().get(AttributeKey.stringKey("operationwebsite"))))
        .isEqualTo("svcB/test");
    assertThat(resultSpanC.getName()).isEqualTo("svcC");
    assertThat(resultSpanD.getName()).isEqualTo("donot/change");
  }

  @Test
  void simpleRenameSpanTestWithLogProcessor() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.SPAN;
    config.id = "SimpleRenameSpan";
    config.name = new NameConfig();
    config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
    SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("db.svc", "location")
            .setAttribute("operation", "get")
            .setAttribute("id", "1234")
            .setAttribute("applicationinsights.internal.log", true)
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are not modified
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);
    assertThat(resultSpan.getName()).isEqualTo("svcA");
  }
}
