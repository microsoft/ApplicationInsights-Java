package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.ConfigurationBuilder.ConfigurationException;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorMatchType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorType;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span;
import org.junit.*;

import static org.junit.Assert.*;

public class ExporterWithAttributeProcessorTest {


    @Test(expected = IllegalArgumentException.class)
    public void noActionTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "noAction";
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inValidConfigTestWithNoValueInActionTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "inValidConfigTestWithNoValueInAction";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inValidConfigTestWithInvalidIncludeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "inValidConfigTestWithInvalidInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test
    public void actionDeleteTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionDelete";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.delete;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNull(resultSpan.getAttributes().get("testKey"));
        assertNotNull(resultSpan.getAttributes().get("one"));
        assertNotNull(resultSpan.getAttributes().get("TESTKEY"));

    }

    @Test
    public void actionInsertTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionInsert";
        ProcessorAction action = new ProcessorAction();
        action.key = "testNewKey";
        action.value = "testNewValue";
        action.action = ProcessorActionType.insert;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotNull(resultSpan.getAttributes().get("testNewKey"));
        assertEquals("testNewValue", Objects.requireNonNull(resultSpan.getAttributes().get("testNewKey")).getStringValue());

    }

    @Test
    public void actionInsertAndUpdateTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionInsertAndUpdate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testNewKey";
        action.value = "testNewValue";
        action.action = ProcessorActionType.insert;
        ProcessorAction updateAction = new ProcessorAction();
        updateAction.key = "testKey";
        updateAction.value = "testNewValue2";
        updateAction.action = ProcessorActionType.update;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        actions.add(updateAction);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotNull(resultSpan.getAttributes().get("testNewKey"));
        assertEquals("testNewValue", Objects.requireNonNull(resultSpan.getAttributes().get("testNewKey")).getStringValue());
        assertEquals("testNewValue2", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());

    }

    @Test
    public void actionInsertWithDuplicateTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionInsertWithDuplicate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.value = "testNewValue";
        action.action = ProcessorActionType.insert;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotEquals("testValue", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());
        assertEquals("testNewValue", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());
        assertEquals("1", Objects.requireNonNull(resultSpan.getAttributes().get("one")).getStringValue());

    }

    @Test
    public void actionInsertFromAttributeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionInsertFromAttribute";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.fromAttribute = "testKey2";
        action.action = ProcessorActionType.insert;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotEquals("testValue", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());
        assertEquals("testValue2", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());
        assertEquals("1", Objects.requireNonNull(resultSpan.getAttributes().get("one")).getStringValue());

    }

    @Test
    public void actionSimpleUpdateTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionSimpleUpdate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("redacted", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void actionUpdateFromAttributeUpdateTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionUpdateFromAttributeUpdate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.fromAttribute = "testKey2";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("testValue2", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void complexActionTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "complexAction";
        ProcessorAction updateAction = new ProcessorAction();
        updateAction.key = "testKey";
        updateAction.action = ProcessorActionType.update;
        updateAction.value = "redacted";
        ProcessorAction deleteAction = new ProcessorAction();
        deleteAction.key = "testKey2";
        deleteAction.action = ProcessorActionType.delete;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(updateAction);
        actions.add(deleteAction);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = OpenTelemetry.getTracer("test").spanBuilder("my span")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("redacted", Objects.requireNonNull(resultSpan.getAttributes().get("testKey")).getStringValue());
        assertNull(resultSpan.getAttributes().get("testKey2"));
    }

    @Test
    public void simpleIncludeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "simpleInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue());
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get("testKey")).getStringValue());
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void simpleIncludeRegexTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "simpleIncludeRegex";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.regexp;
        config.include.spanNames = Arrays.asList("svc.*", "test.*");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("serviceD")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue());
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get("testKey")).getStringValue());
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get("testKey")).getStringValue());
    }

    @Test(expected = ConfigurationException.class)
    public void invalidRegexTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "invalidRegex";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.regexp;
        config.include.spanNames = Collections.singletonList("***");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("serviceD")
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
    }

    @Test
    public void simpleIncludeRegexValueTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "simpleIncludeRegexValue";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.regexp;
        config.include.spanNames = Arrays.asList("svc.*", "test.*");
        ProcessorAttribute attributeWithValue = new ProcessorAttribute();
        attributeWithValue.key = "testKey";
        attributeWithValue.value = "Value.*";
        config.include.attributes = new ArrayList<>();
        config.include.attributes.add(attributeWithValue);
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue2")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("serviceD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanE = OpenTelemetry.getTracer("test").spanBuilder("svcE")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpanA = result.get(0);
        SpanData resultSpanB = result.get(1);
        SpanData resultSpanC = result.get(2);
        SpanData resultSpanE = result.get(4);
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue());
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get("testKey")).getStringValue());
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get("testKey")).getStringValue());
        assertEquals("testV1", Objects.requireNonNull(resultSpanE.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void simpleIncludeHashTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "simpleIncludeHash";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB", "svcC");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.hash;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", 2L)
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", 123)
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        SpanData resultSpanD = result.get(3);
        assertNotEquals("HashValue:" + Objects.requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue(), "testValue", Objects
                .requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue());
        assertEquals(2L, Objects.requireNonNull(resultSpanB.getAttributes().get("testKey")).getLongValue());
        assertEquals("testValue", Objects.requireNonNull(resultSpanD.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void simpleExcludeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "simpleExclude";
        config.exclude = new ProcessorIncludeExclude();
        config.exclude.matchType = ProcessorMatchType.strict;
        config.exclude.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue());
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get("testKey")).getStringValue());
        assertEquals("redacted", Objects.requireNonNull(resultSpanC.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void simpleExcludeRegexTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "simpleExcludeRegex";
        config.exclude = new ProcessorIncludeExclude();
        config.exclude.matchType = ProcessorMatchType.regexp;
        config.exclude.spanNames = Collections.singletonList("svc.*");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("serviceD")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue());
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get("testKey")).getStringValue());
        assertEquals("redacted", Objects.requireNonNull(resultSpanC.getAttributes().get("testKey")).getStringValue());
        assertEquals("redacted", Objects.requireNonNull(resultSpanD.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void multiIncludeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "multiInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        config.include.attributes = new ArrayList<>();
        ProcessorAttribute attributeWithValue = new ProcessorAttribute();
        attributeWithValue.key = "testKey";
        attributeWithValue.value = "testValue";
        ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
        attributeWithNoValue.key = "testKey2";
        config.include.attributes.add(attributeWithValue);
        config.include.attributes.add(attributeWithNoValue);
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.delete;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey3", "testValue3")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertNull(resultSpanA.getAttributes().get("testKey"));
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get("testKey")).getStringValue());
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get("testKey")).getStringValue());
    }

    @Test
    public void multiExcludeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "multiExclude";
        config.exclude = new ProcessorIncludeExclude();
        config.exclude.matchType = ProcessorMatchType.strict;
        config.exclude.spanNames = Arrays.asList("svcA", "svcB");
        config.exclude.attributes = new ArrayList<>();
        ProcessorAttribute attributeWithValue = new ProcessorAttribute();
        attributeWithValue.key = "testKey";
        attributeWithValue.value = "testValue";
        config.exclude.attributes.add(attributeWithValue);
        ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
        attributeWithNoValue.key = "testKey2";
        config.exclude.attributes.add(attributeWithNoValue);
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.delete;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey3", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpanA.getAttributes().get("testKey")).getStringValue());
        assertNull(resultSpanB.getAttributes().get("testKey"));
        assertNull(resultSpanC.getAttributes().get("testKey"));
        assertNull(resultSpanD.getAttributes().get("testKey"));

    }

    @Test
    public void selectiveProcessingTest() { //With both include and exclude
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "selectiveProcessing";
        config.include = new ProcessorIncludeExclude();
        config.exclude = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        config.exclude.matchType = ProcessorMatchType.strict;
        config.exclude.attributes = new ArrayList<>();
        ProcessorAttribute attributeWithValue = new ProcessorAttribute();
        attributeWithValue.key = "testKey";
        attributeWithValue.value = "testValue";
        config.exclude.attributes.add(attributeWithValue);
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey2";
        action.action = ProcessorActionType.delete;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = OpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = OpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = OpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = OpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("testValue2", Objects.requireNonNull(resultSpanA.getAttributes().get("testKey2")).getStringValue());
        assertNull(resultSpanB.getAttributes().get("testKey2"));
        assertEquals("testValue2", Objects.requireNonNull(resultSpanC.getAttributes().get("testKey2")).getStringValue());
    }


}
