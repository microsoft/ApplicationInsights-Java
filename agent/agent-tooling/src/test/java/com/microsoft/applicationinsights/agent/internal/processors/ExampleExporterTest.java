package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorIncludeExclude;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span;
import org.junit.*;

import static org.junit.Assert.*;

public class ExampleExporterTest {

    @Test
    public void noActionTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("1", resultSpan.getAttributes().get("one").getStringValue());
    }

    @Test
    public void wrongActionTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="wrongAction";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("1", resultSpan.getAttributes().get("one").getStringValue());
    }

    @Test
    public void actionDeleteTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="delete";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("TESTKEY","testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNull(resultSpan.getAttributes().get("testKey"));
        assertNotNull(resultSpan.getAttributes().get("one"));
        assertNotNull(resultSpan.getAttributes().get("TESTKEY"));

    }

    @Test
    public void actionInsertTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testNewKey";
        action.value="testNewValue";
        action.action="insert";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("TESTKEY","testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotNull(resultSpan.getAttributes().get("testNewKey"));
        assertEquals("testNewValue",resultSpan.getAttributes().get("testNewKey").getStringValue());

    }

    @Test
    public void actionInsertAndUpdateTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testNewKey";
        action.value="testNewValue";
        action.action="insert";
        SpanProcessorAction updateAction=new SpanProcessorAction();
        updateAction.key="testKey";
        updateAction.value="testNewValue2";
        updateAction.action="update";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        actions.add(updateAction);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("TESTKEY","testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotNull(resultSpan.getAttributes().get("testNewKey"));
        assertEquals("testNewValue",resultSpan.getAttributes().get("testNewKey").getStringValue());
        assertEquals("testNewValue2",resultSpan.getAttributes().get("testKey").getStringValue());

    }

    @Test
    public void actionInsertWithDuplicateTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.value="testNewValue";
        action.action="insert";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("TESTKEY","testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotEquals("testValue",resultSpan.getAttributes().get("testKey").getStringValue());
        assertEquals("testNewValue",resultSpan.getAttributes().get("testKey").getStringValue());
        assertEquals("1",resultSpan.getAttributes().get("one").getStringValue());

    }

    @Test
    public void actionSimpleUpdateTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="update";
        action.value="redacted";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("TESTKEY","testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("redacted",resultSpan.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void actionUpdateFromAttributeUpdateTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="update";
        action.from_attribute="testKey2";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("testValue2",resultSpan.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void complexActionTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        SpanProcessorAction updateAction=new SpanProcessorAction();
        updateAction.key="testKey";
        updateAction.action="update";
        updateAction.value="redacted";
        SpanProcessorAction deleteAction=new SpanProcessorAction();
        deleteAction.key="testKey2";
        deleteAction.action="delete";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(updateAction);
        actions.add(deleteAction);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("redacted",resultSpan.getAttributes().get("testKey").getStringValue());
        assertNull(resultSpan.getAttributes().get("testKey2"));
    }

    @Test
    public void simpleIncludeTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.include = new SpanProcessorIncludeExclude();
        config.include.match_type = "strict";
        config.include.span_names = Arrays.asList("svcA", "svcB");
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="update";
        action.value="redacted";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        assertEquals("redacted",resultSpanA.getAttributes().get("testKey").getStringValue());
        assertEquals("redacted",resultSpanB.getAttributes().get("testKey").getStringValue());
        assertEquals("testValue",resultSpanC.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void simpleIncludeRegexTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.include = new SpanProcessorIncludeExclude();
        config.include.match_type = "regexp";
        config.include.span_names = Arrays.asList("svc.*","test.*");
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="update";
        action.value="redacted";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("serviceD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        assertEquals("redacted",resultSpanA.getAttributes().get("testKey").getStringValue());
        assertEquals("redacted",resultSpanB.getAttributes().get("testKey").getStringValue());
        assertEquals("testValue",resultSpanC.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void simpleIncludeHashTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.include = new SpanProcessorIncludeExclude();
        config.include.match_type = "strict";
        config.include.span_names = Arrays.asList("svcA","svcB","svcC");
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="hash";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey",2L)
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey",123)
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        SpanData resultSpanD = result.get(3);
        assertNotEquals("HashValue:"+resultSpanA.getAttributes().get("testKey").getStringValue(),"testValue",resultSpanA.getAttributes().get("testKey").getStringValue());
        assertEquals(2L,resultSpanB.getAttributes().get("testKey").getLongValue());
        assertEquals("testValue",resultSpanD.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void simpleExcludeTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.exclude = new SpanProcessorIncludeExclude();
        config.exclude.match_type = "strict";
        config.exclude.span_names = Arrays.asList("svcA", "svcB");
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="update";
        action.value="redacted";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        assertEquals("testValue",resultSpanA.getAttributes().get("testKey").getStringValue());
        assertEquals("testValue",resultSpanB.getAttributes().get("testKey").getStringValue());
        assertEquals("redacted",resultSpanC.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void simpleExcludeRegexTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.exclude = new SpanProcessorIncludeExclude();
        config.exclude.match_type = "regexp";
        config.exclude.span_names = Arrays.asList("svc.*");
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="update";
        action.value="redacted";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("serviceD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        assertEquals("testValue",resultSpanA.getAttributes().get("testKey").getStringValue());
        assertEquals("testValue",resultSpanB.getAttributes().get("testKey").getStringValue());
        assertEquals("redacted",resultSpanC.getAttributes().get("testKey").getStringValue());
        assertEquals("redacted",resultSpanD.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void multiIncludeTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.include = new SpanProcessorIncludeExclude();
        config.include.match_type = "strict";
        config.include.span_names = Arrays.asList("svcA", "svcB");
        config.include.attributes= new ArrayList<>();
        SpanProcessorAttribute attributeWithValue=new SpanProcessorAttribute();
        attributeWithValue.key="testKey";
        attributeWithValue.value="testValue";
        SpanProcessorAttribute attributeWithNoValue=new SpanProcessorAttribute();
        attributeWithNoValue.key="testKey2";
        config.include.attributes.add(attributeWithValue);
        config.include.attributes.add(attributeWithNoValue);
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="delete";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey","testValue1")
                .setAttribute("testKey3","testValue3")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        assertNull(resultSpanA.getAttributes().get("testKey"));
        assertEquals("testValue1",resultSpanB.getAttributes().get("testKey").getStringValue());
        assertEquals("testValue",resultSpanC.getAttributes().get("testKey").getStringValue());
    }

    @Test
    public void multiExcludeTest() {
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.exclude = new SpanProcessorIncludeExclude();
        config.exclude.match_type = "strict";
        config.exclude.span_names = Arrays.asList("svcA", "svcB");
        config.exclude.attributes= new ArrayList<>();
        SpanProcessorAttribute attributeWithValue=new SpanProcessorAttribute();
        attributeWithValue.key="testKey";
        attributeWithValue.value="testValue";
        config.exclude.attributes.add(attributeWithValue);
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey";
        action.action="delete";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey","testValue1")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        assertEquals("testValue",resultSpanA.getAttributes().get("testKey").getStringValue());
        assertNull(resultSpanB.getAttributes().get("testKey"));
        assertNull(resultSpanC.getAttributes().get("testKey"));
        assertNull(resultSpanD.getAttributes().get("testKey"));

    }

    @Test
    public void selectiveProcessingTest() { //With both include and exclude
        MockExporter mockExporter = new MockExporter();
        SpanProcessorConfig config = new SpanProcessorConfig();
        config.include = new SpanProcessorIncludeExclude();
        config.exclude = new SpanProcessorIncludeExclude();
        config.include.match_type = "strict";
        config.include.span_names = Arrays.asList("svcA", "svcB");
        config.exclude.match_type = "strict";
        config.exclude.attributes= new ArrayList<>();
        SpanProcessorAttribute attributeWithValue=new SpanProcessorAttribute();
        attributeWithValue.key="testKey";
        attributeWithValue.value="testValue";
        config.exclude.attributes.add(attributeWithValue);
        SpanProcessorAction action=new SpanProcessorAction();
        action.key="testKey2";
        action.action="delete";
        List<SpanProcessorAction> actions=new ArrayList<>();
        actions.add(action);
        config.actions=actions;
        SpanExporter exampleExporter = ExampleExporter.create(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey","testValue1")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey","testValue")
                .setAttribute("testKey2","testValue2")
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
        assertEquals("testValue2",resultSpanA.getAttributes().get("testKey2").getStringValue());
        assertNull(resultSpanB.getAttributes().get("testKey2"));
        assertEquals("testValue2",resultSpanC.getAttributes().get("testKey2").getStringValue());
    }







}
