package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.BaseSampleSourceTelemetry;
import com.microsoft.applicationinsights.telemetry.BaseTelemetry;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.localforwarder.library.inputs.contracts.DataPointType;
import com.microsoft.localforwarder.library.inputs.contracts.ExceptionDetails;
import com.microsoft.localforwarder.library.inputs.contracts.ExceptionDetails.Builder;
import com.microsoft.localforwarder.library.inputs.contracts.SeverityLevel;
import com.microsoft.localforwarder.library.inputs.contracts.StackFrame;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.mockito.internal.matchers.Null;
import sun.misc.Perf;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Testing the utility methods in {@link LocalForwarderModelTransformer}
 */
public class LocalForwarderModelTransformerUtilityTests {

    private static com.microsoft.applicationinsights.internal.schemav2.StackFrame generateRandomStackFrame() {
        final Random rand = new Random(System.currentTimeMillis());
        final com.microsoft.applicationinsights.internal.schemav2.StackFrame rval = new com.microsoft.applicationinsights.internal.schemav2.StackFrame();
        rval.setAssembly(String.format("assembly-%s", RandomStringUtils.random(8, true, true)));
        rval.setFileName(String.format("%s.txt", RandomStringUtils.random(10, true, true)));
        rval.setMethod(String.format("testMethod_%s()", RandomStringUtils.random(8, true, true)));
        rval.setLevel(rand.nextInt(50));
        rval.setLine(rand.nextInt(2048));
        return rval;
    }

    @Test
    public void stackFrameTransformerFunctionTransformsAllFieldsProperly() {
        final com.microsoft.applicationinsights.internal.schemav2.StackFrame sf = generateRandomStackFrame();

        assertEquals(StackFrame.newBuilder()
                        .setAssembly(sf.getAssembly())
                        .setFileName(sf.getFileName())
                        .setLevel(sf.getLevel())
                        .setLine(sf.getLine())
                        .setMethod(sf.getMethod())
                        .build(),
                LocalForwarderModelTransformer.STACK_FRAME_TRANSFORMER_FUNCTION.apply(sf));
    }

    @Test
    public void stackFrameTransformerFunctionTransformsProperlyWithSomeNullValues() {
        final int level = 42;
        final int line = 201;

        final com.microsoft.applicationinsights.internal.schemav2.StackFrame sf = new com.microsoft.applicationinsights.internal.schemav2.StackFrame();
        sf.setLevel(level);
        sf.setLine(line);

        assertEquals(StackFrame.newBuilder()
                        .setLevel(level)
                        .setLine(line)
                        .build(),
                LocalForwarderModelTransformer.STACK_FRAME_TRANSFORMER_FUNCTION.apply(sf));
    }

    @Test
    public void exceptionDetailsTransformerFunctionTransformsProperlyWithAllFieldsSet() {
        final int id = 778899;
        final int outerId = 112233;
        final boolean hasFullStack = false;
        final String typeName = "test-type";
        final String message = "this is only a test";
        final String stack = "test-stack";


        final List<com.microsoft.applicationinsights.internal.schemav2.StackFrame> parsedStack = new ArrayList<com.microsoft.applicationinsights.internal.schemav2.StackFrame>() {{
            add(generateRandomStackFrame());
            add(generateRandomStackFrame());
            add(generateRandomStackFrame());
        }};

        final com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails ed = new com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails();
        ed.setHasFullStack(hasFullStack);
        ed.setId(id);
        ed.setOuterId(outerId);
        ed.setMessage(message);
        ed.setTypeName(typeName);
        ed.setStack(stack);
        ed.setParsedStack(parsedStack);

        final Builder edBuilder = ExceptionDetails.newBuilder()
                .setHasFullStack(BoolValue.of(hasFullStack))
                .setId(id)
                .setOuterId(outerId)
                .setStack(stack)
                .setMessage(message)
                .setTypeName(typeName);
        for (com.microsoft.applicationinsights.internal.schemav2.StackFrame sf : parsedStack) {
            edBuilder.addParsedStack(StackFrame.newBuilder()
                    .setLine(sf.getLine())
                    .setLevel(sf.getLevel())
                    .setFileName(sf.getFileName())
                    .setAssembly(sf.getAssembly())
                    .setMethod(sf.getMethod()));
        }

        assertEquals(edBuilder.build(), LocalForwarderModelTransformer.EXCEPTION_DETAILS_TRANSFORMER_FUNCTION.apply(ed));
    }

    @Test
    public void transformSeverityLevelMapsCorrectly() {
        assertEquals(SeverityLevel.Critical, LocalForwarderModelTransformer.transformSeverityLevel(com.microsoft.applicationinsights.telemetry.SeverityLevel.Critical));
        assertEquals(SeverityLevel.Error, LocalForwarderModelTransformer.transformSeverityLevel(com.microsoft.applicationinsights.telemetry.SeverityLevel.Error));
        assertEquals(SeverityLevel.Warning, LocalForwarderModelTransformer.transformSeverityLevel(com.microsoft.applicationinsights.telemetry.SeverityLevel.Warning));
        assertEquals(SeverityLevel.Information, LocalForwarderModelTransformer.transformSeverityLevel(com.microsoft.applicationinsights.telemetry.SeverityLevel.Information));
        assertEquals(SeverityLevel.Verbose, LocalForwarderModelTransformer.transformSeverityLevel(com.microsoft.applicationinsights.telemetry.SeverityLevel.Verbose));
        assertEquals(SeverityLevel.UNRECOGNIZED, LocalForwarderModelTransformer.transformSeverityLevel(null));
    }

    @Test
    public void transformDataPointTypeMapsCorrectly() {
        assertEquals(DataPointType.Aggregation, LocalForwarderModelTransformer.transformDataPointType(com.microsoft.applicationinsights.internal.schemav2.DataPointType.Aggregation));
        assertEquals(DataPointType.Measurement, LocalForwarderModelTransformer.transformDataPointType(com.microsoft.applicationinsights.internal.schemav2.DataPointType.Measurement));
        assertEquals(DataPointType.UNRECOGNIZED, LocalForwarderModelTransformer.transformDataPointType(null));
    }

    @Test
    public void transformDurationMapsCorrectly() {
        assertEquals(Duration.newBuilder()
                .setSeconds(0)
                .setNanos(0)
                .build(),
                LocalForwarderModelTransformer.transformDuration(new com.microsoft.applicationinsights.telemetry.Duration(0)));
        assertEquals(Duration.newBuilder().setSeconds(86400).setNanos(0).build(),
                LocalForwarderModelTransformer.transformDuration(new com.microsoft.applicationinsights.telemetry.Duration(1, 0, 0, 0, 0)));
        assertEquals(Duration.newBuilder().setSeconds(3600).setNanos(0).build(),
                LocalForwarderModelTransformer.transformDuration(new com.microsoft.applicationinsights.telemetry.Duration(0, 1, 0, 0, 0)));
        assertEquals(Duration.newBuilder().setSeconds(60).setNanos(0).build(),
                LocalForwarderModelTransformer.transformDuration(new com.microsoft.applicationinsights.telemetry.Duration(0, 0, 1, 0, 0)));
        assertEquals(Duration.newBuilder().setSeconds(1).setNanos(0).build(),
                LocalForwarderModelTransformer.transformDuration(new com.microsoft.applicationinsights.telemetry.Duration(0, 0, 0, 1, 0)));
        assertEquals(Duration.newBuilder().setSeconds(0).setNanos(1_000_000).build(),
                LocalForwarderModelTransformer.transformDuration(new com.microsoft.applicationinsights.telemetry.Duration(0, 0, 0, 0, 1)));
    }

    @Test(expected = NullPointerException.class)
    public void transformDurationDoesNotAcceptNull() {
        LocalForwarderModelTransformer.transformDuration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void telemetryBuilderWithStandardFieldsDoesNotAllowNullInstrumentationKey() {
        LocalForwarderModelTransformer.telemetryBuilderWithStandardFields(new MetricTelemetry());
    }

    @Test
    public void telemetryBuilderWithStandardFieldsMinimalFields() {
        String ikey = "123";
        for (BaseTelemetry<?> bt : new BaseTelemetry[] {
                new MetricTelemetry(),
                new TraceTelemetry(),
                new ExceptionTelemetry(new RuntimeException("fake")),
                new RemoteDependencyTelemetry(),
                new EventTelemetry(),
                new PageViewTelemetry(),
                new RequestTelemetry(),
                new PerformanceCounterTelemetry()
        }) {
            String envelopeName = bt instanceof PerformanceCounterTelemetry ? MetricTelemetry.ENVELOPE_NAME : bt.getEnvelopName();
            bt.getContext().setInstrumentationKey(ikey);
            final Date date = new Date();
            bt.setTimestamp(date);
            assertEquals(Telemetry.newBuilder()
                            .setDateTime(LocalStringsUtils.getDateFormatter().format(date))
                            .setVer(LocalForwarderModelTransformer.CURRENT_ENVELOPE_VERSION)
                            .setInstrumentationKey(ikey)
                            .setDataTypeName(String.format("Microsoft.ApplicationInsights.%s.%s", ikey, envelopeName)).buildPartial(),
                    LocalForwarderModelTransformer.telemetryBuilderWithStandardFields(bt).buildPartial());
        }
    }

    @Test
    public void telemetryBuilderWithStandardFieldsMaximalFields() {
        String ikey = "123";
        for (BaseTelemetry<?> bt : new BaseTelemetry[] {
                new MetricTelemetry(),
                new TraceTelemetry(),
                new ExceptionTelemetry(new RuntimeException("fake")),
                new RemoteDependencyTelemetry(),
                new EventTelemetry(),
                new PageViewTelemetry(),
                new RequestTelemetry(),
                new PerformanceCounterTelemetry()
        }) {
            String envelopeName = bt instanceof PerformanceCounterTelemetry ? MetricTelemetry.ENVELOPE_NAME : bt.getEnvelopName();
            bt.getContext().setInstrumentationKey(ikey);

            final Date date = new Date();
            bt.setTimestamp(date);

            final String sequence = RandomStringUtils.random(4, false, true); // exclude letters
            bt.setSequence(sequence);

            Map<String, String> tags = new HashMap<String, String>() {{
                put("tag1", "something");
                put("tag2", "something else");
            }};
            bt.getContext().getTags().putAll(tags);

            final double samplingPercentage = 0.789;

            final Telemetry.Builder expected = Telemetry.newBuilder()
                    .setDateTime(LocalStringsUtils.getDateFormatter().format(date))
                    .setVer(LocalForwarderModelTransformer.CURRENT_ENVELOPE_VERSION)
                    .setInstrumentationKey(ikey)
                    .setSequenceNumber(sequence)
                    .putAllTags(tags)
                    .setDataTypeName(String.format("Microsoft.ApplicationInsights.%s.%s", ikey, envelopeName));

            if (bt instanceof BaseSampleSourceTelemetry) {
                ((BaseSampleSourceTelemetry)bt).setSamplingPercentage(samplingPercentage);
                expected.setSamplingRate(DoubleValue.of(samplingPercentage));
            }

            assertEquals(expected.buildPartial(), LocalForwarderModelTransformer.telemetryBuilderWithStandardFields(bt).buildPartial());
        }
    }
}
