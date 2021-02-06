package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ExtractAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorMatchType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.ProcessorActionAdaptor;
import com.microsoft.applicationinsights.agent.bootstrap.customExceptions.FriendlyException;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.api.trace.Span;
import org.junit.*;

import static org.junit.Assert.*;

public class ExporterWithAttributeProcessorTest {


    @Test(expected = FriendlyException.class)
    public void noActionTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "noAction";
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test(expected = FriendlyException.class)
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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test(expected = FriendlyException.class)
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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")));
        assertNotNull(resultSpan.getAttributes().get(AttributeKey.stringKey("one")));
        assertNotNull(resultSpan.getAttributes().get(AttributeKey.stringKey("TESTKEY")));

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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertNotNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")));
        assertEquals("testNewValue", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))));

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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertNotNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")));
        assertEquals("testNewValue", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))));
        assertEquals("testNewValue2", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));

    }

    @Test
    public void actionInsertAndUpdateSameAttributeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionInsertAndUpdate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testNewKey";
        action.value = "testNewValue";
        action.action = ProcessorActionType.insert;
        ProcessorAction updateAction = new ProcessorAction();
        updateAction.key = "testNewKey";
        updateAction.value = "testNewValue2";
        updateAction.action = ProcessorActionType.update;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        actions.add(updateAction);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertNotNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")));
        assertEquals("testNewValue2", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))));

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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertNotEquals("testNewValue", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("1", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))));

    }

    @Test
    public void actionInsertFromAttributeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionInsertFromAttribute";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey3";
        action.fromAttribute = "testKey2";
        action.action = ProcessorActionType.insert;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue2", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey3"))));
        assertEquals("1", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))));

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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertEquals("testValue2", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey2")));
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceD")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test(expected = FriendlyException.class)
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceD")
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue2")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanE = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcE")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testV1", Objects.requireNonNull(resultSpanE.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test
    public void simpleIncludeRegexNoValueTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "simpleIncludeRegexNoValue";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = ProcessorMatchType.regexp;
        config.include.spanNames = Arrays.asList("svc.*", "test.*");
        ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
        attributeWithNoValue.key = "testKey";
        config.include.attributes = new ArrayList<>();
        config.include.attributes.add(attributeWithNoValue);
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue2")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanE = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcE")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanE.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", 2L)
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", 123)
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertNotEquals("HashValue:" + Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))), "testValue", Objects
                .requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue2", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey2"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("serviceD")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey3", "testValue3")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")));
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey3", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")));
        assertNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")));
        assertNull(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")));

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

        Span spanA = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = GlobalOpenTelemetry.getTracer("test").spanBuilder("svcD")
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
        assertEquals("testValue2", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey2"))));
        assertNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey2")));
        assertEquals("testValue2", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey2"))));
    }

    @Test
    public void actionInsertWithExtractTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionExtract";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        String regex="^(?<httpProtocol>.*):\\/\\/(?<httpDomain>.*)\\/(?<httpPath>.*)(\\?|\\&)(?<httpQueryParams>.*)";
        Pattern pattern = Pattern.compile(regex);
        List<String> groupNames= ProcessorActionAdaptor.getGroupNames(regex);
        action.extractAttribute= new ExtractAttribute(pattern,groupNames);
        action.action = ProcessorActionType.extract;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "http://example.com/path?queryParam1=value1,queryParam2=value2")
                .setAttribute("TESTKEY", "testValue2")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("http", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpProtocol"))));
        assertEquals("example.com", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpDomain"))));
        assertEquals("path", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath"))));
        assertEquals("queryParam1=value1,queryParam2=value2",
                Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpQueryParams"))));
        assertEquals("http://example.com/path?queryParam1=value1,queryParam2=value2",
                Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test
    public void actionInsertWithExtractDuplicateTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.processorName = "actionExtract";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        String regex="^(?<httpProtocol>.*):\\/\\/(?<httpDomain>.*)\\/(?<httpPath>.*)(\\?|\\&)(?<httpQueryParams>.*)";
        Pattern pattern = Pattern.compile(regex);
        List<String> groupNames= ProcessorActionAdaptor.getGroupNames(regex);
        action.extractAttribute= new ExtractAttribute(pattern,groupNames);
        action.action = ProcessorActionType.extract;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "http://example.com/path?queryParam1=value1,queryParam2=value2")
                .setAttribute("httpPath", "oldPath")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("http", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpProtocol"))));
        assertEquals("example.com", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpDomain"))));
        assertEquals("path", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath"))));
        assertNotEquals("oldPath", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath"))));
        assertEquals("queryParam1=value1,queryParam2=value2",
                Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("httpQueryParams"))));
        assertEquals("http://example.com/path?queryParam1=value1,queryParam2=value2",
                Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

}
