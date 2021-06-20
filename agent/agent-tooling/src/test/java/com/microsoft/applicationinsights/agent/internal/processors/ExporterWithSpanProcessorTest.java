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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.NameConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ToAttributeConfig;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExporterWithSpanProcessorTest {

    private final Tracer tracer = OpenTelemetrySdk.builder().build().getTracer("test");

    @Test
    public void noNameObjectTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.SPAN;
        config.id = "noNameObjectTest";

        assertThatThrownBy(() -> new ExporterWithSpanProcessor(config, mockExporter))
                .isInstanceOf(FriendlyException.class);
    }

    @Test
    public void inValidConfigTestWithNoFromOrToAttributesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.SPAN;
        config.id = "inValidConfigTestWithToAttributesNoRules";
        config.name = new NameConfig();

        assertThatThrownBy(() -> new ExporterWithSpanProcessor(config, mockExporter))
                .isInstanceOf(FriendlyException.class);
    }

    @Test
    public void inValidConfigTestWithToAttributesNoRulesTest() {
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
    public void SimpleRenameSpanTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.SPAN;
        config.id = "SimpleRenameSpan";
        config.name = new NameConfig();
        config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
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
        assertEquals("locationget1234", resultSpan.getName());
    }

    @Test
    public void SimpleRenameSpanWithSeparatorTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.SPAN;
        config.id = "SimpleRenameSpanWithSeparator";
        config.name = new NameConfig();
        config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        config.name.separator = "::";
        SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
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
        assertEquals("location::get::1234", resultSpan.getName());
    }

    @Test
    public void SimpleRenameSpanWithMissingKeysTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.SPAN;
        config.id = "SimpleRenameSpanWithMissingKeys";
        config.name = new NameConfig();
        config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        config.name.separator = "::";
        SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
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
        assertEquals("svcA", resultSpan.getName());

    }

    @Test
    public void RenameSpanWithIncludeTest() {
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

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();

        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();
        Span spanD = tracer.spanBuilder("svcD")
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
        assertEquals("locationget1234", resultSpanA.getName());
        assertEquals("locationget1234", resultSpanB.getName());
        assertEquals("svcC", resultSpanC.getName());
        assertEquals("svcD", resultSpanD.getName());

    }

    @Test
    public void InvalidRegexInRulesTest() {
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
    public void SimpleToAttributesTest() {
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

        Span span = tracer.spanBuilder("/api/v1/document/12345678/update")
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
        assertNotNull(Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))));
        assertEquals("12345678", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))));
        assertEquals("/api/v1/document/{documentId}/update", resultSpan.getName());
    }

    @Test
    public void MultiRuleToAttributesTest() {
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

        Span spanA = tracer.spanBuilder("yyyPassword=123 aba Pass=555 xyx Pass=777 zzz")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();
        Span spanB = tracer.spanBuilder("yyyPassword=**** aba")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .setAttribute("password","234")
                .startSpan();

        SpanData spanAData = ((ReadableSpan) spanA).toSpanData();
        SpanData spanBData = ((ReadableSpan) spanB).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanAData);
        spans.add(spanBData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpanA = result.get(0);
        SpanData resultSpanB = result.get(1);
        assertNotNull(Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertEquals("123", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertNotNull(Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password2"))));
        assertEquals("555", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password2"))));
        assertEquals("yyyPassword={password1} aba Pass={password2} xyx Pass=777 zzz", resultSpanA.getName());
        assertNotNull(Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertEquals("****", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertEquals("yyyPassword={password1} aba", resultSpanB.getName());
    }

    @Test
    public void ExtractAttributesWithIncludeExcludeTest() {
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

        Span spanA = tracer.spanBuilder("svcA/test")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();

        Span spanB = tracer.spanBuilder("svcB/test")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();
        Span spanD = tracer.spanBuilder("donot/change")
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
        assertEquals("{operationwebsite}", resultSpanA.getName());
        assertNotNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("operationwebsite")));
        assertEquals("svcA/test", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("operationwebsite"))));
        assertEquals("{operationwebsite}", resultSpanB.getName());
        assertNotNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("operationwebsite")));
        assertEquals("svcB/test", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("operationwebsite"))));
        assertEquals("svcC", resultSpanC.getName());
        assertEquals("donot/change", resultSpanD.getName());

    }

    @Test
    public void SimpleRenameSpanTestWithLogProcessor() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.SPAN;
        config.id = "SimpleRenameSpan";
        config.name = new NameConfig();
        config.name.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        SpanExporter exampleExporter = new ExporterWithSpanProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
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
        assertEquals("svcA", resultSpan.getName());

    }
}
