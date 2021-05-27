package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ExtractAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.ProcessorActionAdaptor;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.api.trace.Span;
import org.junit.*;

import static org.junit.Assert.*;

public class ExporterWithAttributeProcessorTest {

    private final Tracer tracer = OpenTelemetrySdk.builder().build().getTracer("test");

    @Test(expected = FriendlyException.class)
    public void noActionTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "noAction";
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my span")
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
        config.id = "inValidConfigTestWithNoValueInAction";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
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
        config.id = "inValidConfigTestWithInvalidInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
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
        config.id = "actionDelete";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.delete;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my span")
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
        config.id = "actionInsert";
        ProcessorAction action = new ProcessorAction();
        action.key = "testNewKey";
        action.value = "testNewValue";
        action.action = ProcessorActionType.insert;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my span")
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
        config.id = "actionInsertAndUpdate";
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

        Span span = tracer.spanBuilder("my span")
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
        config.id = "actionInsertAndUpdate";
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

        Span span = tracer.spanBuilder("my span")
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
        config.id = "actionInsertWithDuplicate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.value = "testNewValue";
        action.action = ProcessorActionType.insert;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my span")
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
        config.id = "actionInsertFromAttribute";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey3";
        action.fromAttribute = "testKey2";
        action.action = ProcessorActionType.insert;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my span")
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
        config.id = "actionSimpleUpdate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my span")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("TESTKEY", "testValue2")
                .startSpan();

        Span log = tracer.spanBuilder("my log")
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
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        SpanData resultLog = result.get(1);
        assertEquals("redacted", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultLog.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }


    @Test
    public void actionUpdateFromAttributeUpdateTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "actionUpdateFromAttributeUpdate";
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.fromAttribute = "testKey2";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my span")
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
        config.id = "complexAction";
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

        Span span = tracer.spanBuilder("my span")
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
        config.id = "simpleInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test
    public void simpleIncludeWithLogNamesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "simpleInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
        config.include.logNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        // make sure spanB is not updated since it is not of type log
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test
    public void simpleIncludeWithLogNamesAndSpanNamesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "simpleInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
        config.include.spanNames = Arrays.asList("svcA");
        config.include.logNames = Arrays.asList("logA");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("logA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();
        Span spanB = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
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
        assertEquals("redacted", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        // make sure spanB is not updated since it is not of type log
        assertEquals("redacted", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test
    public void simpleIncludeRegexTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "simpleIncludeRegex";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.regexp;
        config.include.spanNames = Arrays.asList("svc.*", "test.*");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = tracer.spanBuilder("serviceD")
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
    public void simpleIncludeWithLogNamesRegexTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "simpleIncludeRegex";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.regexp;
        config.include.logNames = Arrays.asList("svc.*", "test.*");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();
        Span spanC = tracer.spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();
        Span spanD = tracer.spanBuilder("serviceD")
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
        config.id = "invalidRegex";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.regexp;
        config.include.spanNames = Collections.singletonList("***");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = tracer.spanBuilder("serviceD")
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
        config.id = "simpleIncludeRegexValue";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.regexp;
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

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue2")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = tracer.spanBuilder("serviceD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanE = tracer.spanBuilder("svcE")
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
        config.id = "simpleIncludeRegexNoValue";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.regexp;
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

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue2")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = tracer.spanBuilder("serviceD")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanE = tracer.spanBuilder("svcE")
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
        config.id = "simpleIncludeHash";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB", "svcC");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.hash;
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", 2L)
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", 123)
                .setAttribute("testKey2", "testValue2")
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
        config.id = "simpleExclude";
        config.exclude = new ProcessorIncludeExclude();
        config.exclude.matchType = MatchType.strict;
        config.exclude.spanNames = Arrays.asList("svcA", "svcB");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
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
        assertEquals("testValue", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("redacted", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test
    public void simpleExcludeRegexTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "simpleExcludeRegex";
        config.exclude = new ProcessorIncludeExclude();
        config.exclude.matchType = MatchType.regexp;
        config.exclude.spanNames = Collections.singletonList("svc.*");
        ProcessorAction action = new ProcessorAction();
        action.key = "testKey";
        action.action = ProcessorActionType.update;
        action.value = "redacted";
        List<ProcessorAction> actions = new ArrayList<>();
        actions.add(action);
        config.actions = actions;
        SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("serviceC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanD = tracer.spanBuilder("serviceD")
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
        config.id = "multiInclude";
        config.include = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
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

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey3", "testValue3")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
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
        assertNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")));
        assertEquals("testValue", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))));
        assertEquals("testValue", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))));
    }

    @Test
    public void multiExcludeTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "multiExclude";
        config.exclude = new ProcessorIncludeExclude();
        config.exclude.matchType = MatchType.strict;
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

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey3", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
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
        config.id = "selectiveProcessing";
        config.include = new ProcessorIncludeExclude();
        config.exclude = new ProcessorIncludeExclude();
        config.include.matchType = MatchType.strict;
        config.include.spanNames = Arrays.asList("svcA", "svcB");
        config.exclude.matchType = MatchType.strict;
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

        Span spanA = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanB = tracer.spanBuilder("svcB")
                .setAttribute("one", "1")
                .setAttribute("testKey", "testValue1")
                .setAttribute("testKey2", "testValue2")
                .startSpan();
        Span spanC = tracer.spanBuilder("svcC")
                .setAttribute("two", 2L)
                .setAttribute("testKey", "testValue")
                .setAttribute("testKey2", "testValue2")
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
        assertEquals("testValue2", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey2"))));
        assertNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey2")));
        assertEquals("testValue2", Objects.requireNonNull(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey2"))));
    }

    @Test
    public void actionInsertWithExtractTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.attribute;
        config.id = "actionExtract";
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

        Span span = tracer.spanBuilder("my span")
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
        config.id = "actionExtract";
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

        Span span = tracer.spanBuilder("my span")
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
