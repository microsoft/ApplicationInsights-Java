package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Int32Value;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.BaseTelemetry;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.localforwarder.library.inputs.contracts.DataPoint;
import com.microsoft.localforwarder.library.inputs.contracts.DataPointType;
import com.microsoft.localforwarder.library.inputs.contracts.Dependency;
import com.microsoft.localforwarder.library.inputs.contracts.Event;
import com.microsoft.localforwarder.library.inputs.contracts.Exception;
import com.microsoft.localforwarder.library.inputs.contracts.ExceptionDetails;
import com.microsoft.localforwarder.library.inputs.contracts.Message;
import com.microsoft.localforwarder.library.inputs.contracts.Metric;
import com.microsoft.localforwarder.library.inputs.contracts.PageView;
import com.microsoft.localforwarder.library.inputs.contracts.Request;
import com.microsoft.localforwarder.library.inputs.contracts.StackFrame;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class LocalForwarderModelTranformerTests {

    private static Telemetry.Builder getTelemetryBuilder(String envelopeName, String iKey) {
        return Telemetry.newBuilder()
                .setVer(Envelope.DEFAULT_VER)
                .setInstrumentationKey(iKey)
                .setDataTypeName(LocalForwarderModelTransformer.generateDataTypeName(iKey, envelopeName));
    }

    @Parameters(name = "{index}:{0}")
    public static Collection<Object[]> data() throws MalformedURLException, URISyntaxException {
        List<Object[]> testCases = new ArrayList<>();

        String iKey = UUID.randomUUID().toString();
        Map<String, String> props = new HashMap<String, String>() {{
            put("key-1", "val-1");
            put("key-2", "val-2");
        }};

        Map<String, Double> metrics = new HashMap<String, Double>() {{
            put("key-1", Double.valueOf(1.01));
            put("key-2", Double.valueOf(-2.02));
        }};

        Date timestamp = new Date(0);
        String timestampString = LocalStringsUtils.getDateFormatter().format(timestamp);


        // Metric
        MetricTelemetry mt = new MetricTelemetry("TestMeasurement", 1.123);
        mt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Metric measurement",
                mt,
                getTelemetryBuilder(MetricTelemetry.ENVELOPE_NAME, iKey).setMetric(
                        Metric.newBuilder()
                                .setVer(mt.getVer())
                                .addMetrics(
                                        DataPoint.newBuilder()
                                                .setValue(1.123)
                                                .setName("TestMeasurement")
                                                .setKind(DataPointType.Measurement)
                                ).build()
                ).build()
        });

        // with data: 1.0, 2.1, 3.2, 4.3
        final double sum = 10.6;
        final double stdDev = 1.42;
        final double min = 1.0;
        final double max = 4.3;
        final int count = 4;
        mt = new MetricTelemetry("TestAggregation", sum);
        mt.getContext().setInstrumentationKey(iKey);
        mt.setStandardDeviation(stdDev);
        mt.setMin(min);
        mt.setMax(max);
        mt.setCount(count);
        testCases.add(new Object[]{
                "Metric aggregation",
                mt,
                getTelemetryBuilder(MetricTelemetry.ENVELOPE_NAME, iKey).setMetric(
                        Metric.newBuilder()
                                .setVer(mt.getVer())
                                .addMetrics(
                                        DataPoint.newBuilder()
                                                .setName("TestAggregation")
                                                .setValue(sum)
                                                .setStdDev(DoubleValue.of(stdDev))
                                                .setMax(DoubleValue.of(max))
                                                .setMin(DoubleValue.of(min))
                                                .setCount(Int32Value.of(count))
                                                .setKind(DataPointType.Aggregation)
                                ).build()
                ).build()
        });

        mt = new MetricTelemetry("TestMeasurement-withProps", 2.22222);
        mt.getProperties().putAll(props);
        mt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Metric measurement with properties",
                mt,
                getTelemetryBuilder(MetricTelemetry.ENVELOPE_NAME, iKey).setMetric(
                        Metric.newBuilder()
                                .setVer(mt.getVer())
                                .putAllProperties(props)
                                .addMetrics(
                                        DataPoint.newBuilder()
                                                .setValue(2.22222)
                                                .setName("TestMeasurement-withProps")
                                                .setKind(DataPointType.Measurement)
                                )
                ).build()
        });

        // Request
        String name = "fake-request";
        Duration duration = new Duration(4321L);
        String responseCode = "201";
        boolean success = true;
        RequestTelemetry rt = new RequestTelemetry(name, timestamp, duration, responseCode, success);
        rt.setSequence("123");
        rt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Request",
                rt,
                getTelemetryBuilder(RequestTelemetry.ENVELOPE_NAME, iKey)
                        .setSequenceNumber("123")
                        .setDateTime(timestampString)
                        .setRequest(
                        Request.newBuilder()
                            .setVer(rt.getVer())
                            .setId(rt.getId())
                            .setName(name)
                            .setDuration(LocalForwarderModelTransformer.transformDuration(duration))
                            .setResponseCode(responseCode)
                            .setSuccess(BoolValue.of(success))
                        )
                        .build()
        });

        name = "fake-request2";
        duration = new Duration(789L);
        responseCode = "404";
        success = false;
        rt = new RequestTelemetry(name, timestamp, duration, responseCode, success);
        rt.setSequence("321");
        rt.setSource("fake-source");
        rt.setUrl(new URL("http://a.b.com"));
        rt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Request with source & url",
                rt,
                getTelemetryBuilder(RequestTelemetry.ENVELOPE_NAME, iKey)
                        .setSequenceNumber("321")
                        .setDateTime(timestampString)
                        .setRequest(
                                Request.newBuilder()
                                        .setVer(rt.getVer())
                                        .setId(rt.getId())
                                        .setSource("fake-source")
                                        .setUrl(rt.getUrlString())
                                        .setName(name)
                                        .setDuration(LocalForwarderModelTransformer.transformDuration(duration))
                                        .setResponseCode(responseCode)
                                        .setSuccess(BoolValue.of(success))
                        )
                        .build()
        });

        name = "fake-request-3";
        duration = new Duration(405L);
        responseCode = "207";
        success = true;
        rt = new RequestTelemetry(name, timestamp, duration, responseCode, success);
        rt.setSamplingPercentage(Double.valueOf(0.999));
        rt.getProperties().putAll(props);
        rt.getMetrics().putAll(metrics);
        rt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Request with Properties and Metrics",
                rt,
                getTelemetryBuilder(RequestTelemetry.ENVELOPE_NAME, iKey)
                        .setDateTime(timestampString)
                        .setSamplingRate(DoubleValue.of(0.999))
                        .setRequest(
                                Request.newBuilder()
                                        .setVer(rt.getVer())
                                        .setId(rt.getId())
                                        .setName(name)
                                        .setDuration(LocalForwarderModelTransformer.transformDuration(duration))
                                        .setResponseCode(responseCode)
                                        .setSuccess(BoolValue.of(success))
                                        .putAllProperties(props)
                                        .putAllMeasurements(metrics)
                        )
                        .build()
        });

        // Event
        name = "MyEvent";
        EventTelemetry et = new EventTelemetry(name);
        et.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Event",
                et,
                getTelemetryBuilder(EventTelemetry.ENVELOPE_NAME, iKey)
                        .setEvent(Event.newBuilder()
                                .setVer(et.getVer())
                                .setName(name)
                        )
                        .build()
        });

        name = "MyEvent-2";
        et = new EventTelemetry(name);
        et.getContext().setInstrumentationKey(iKey);
        et.getProperties().putAll(props);
        et.getMetrics().putAll(metrics);
        testCases.add(new Object[]{
                "Event with props & metrics",
                et,
                getTelemetryBuilder(EventTelemetry.ENVELOPE_NAME, iKey)
                        .setEvent(Event.newBuilder()
                                .setVer(et.getVer())
                                .setName(name)
                                .putAllProperties(props)
                                .putAllMeasurements(metrics)
                        )
                        .build()
        });

        // PageView
        name = "the vanilla page";
        PageViewTelemetry pvt = new PageViewTelemetry(name);
        pvt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "PageView, simple",
                pvt,
                getTelemetryBuilder(PageViewTelemetry.ENVELOPE_NAME, iKey)
                        .setPageView(PageView.newBuilder()
                                .setEvent(Event.newBuilder()
                                        .setVer(pvt.getVer())
                                        .setName(name))
                                .setDuration(LocalForwarderModelTransformer.transformDuration(pvt.getDurationObject()))
                ).build()
        });

        name = "another fake page";
        pvt = new PageViewTelemetry(name);
        pvt.getContext().setInstrumentationKey(iKey);
        pvt.getProperties().putAll(props);
        pvt.getMetrics().putAll(metrics);
        String urlString = "http://fake.com/why/is/this/uri?clue=false";
        pvt.setUrl(new URI(urlString));
        pvt.setDuration(665544L);
        testCases.add(new Object[]{
                "PageView, with all fields",
                pvt,
                getTelemetryBuilder(PageViewTelemetry.ENVELOPE_NAME, iKey)
                        .setPageView(PageView.newBuilder()
                                .setEvent(Event.newBuilder()
                                        .setVer(pvt.getVer())
                                        .setName(name)
                                        .putAllMeasurements(metrics)
                                        .putAllProperties(props)
                                )
                                .setDuration(LocalForwarderModelTransformer.transformDuration(pvt.getDurationObject()))
                                .setUrl(urlString)
                        ).build()
        });

        // Trace
        String message = "nothing to see here";
        TraceTelemetry tt = new TraceTelemetry(message, SeverityLevel.Information);
        tt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Trace",
                tt,
                getTelemetryBuilder(TraceTelemetry.ENVELOPE_NAME, iKey)
                        .setMessage(Message.newBuilder()
                                .setVer(tt.getVer())
                                .setMessage(message)
                                .setSeverityLevel(LocalForwarderModelTransformer.transformSeverityLevel(tt.getSeverityLevel()))
                        )
                        .build()
        });

        message = "still nothing, but with more emphasis";
        tt = new TraceTelemetry(message, SeverityLevel.Error);
        tt.getContext().setInstrumentationKey(iKey);
        tt.getProperties().putAll(props);
        testCases.add(new Object[]{
                "Trace, with props",
                tt,
                getTelemetryBuilder(TraceTelemetry.ENVELOPE_NAME, iKey)
                        .setMessage(Message.newBuilder()
                                .setVer(tt.getVer())
                                .setMessage(message)
                                .setSeverityLevel(LocalForwarderModelTransformer.transformSeverityLevel(tt.getSeverityLevel()))
                                .putAllProperties(props)
                        )
                        .build()
        });

        // Dependency
        String dependencyName = "fake-dependency-name";
        String commandName = "command name?";
        duration = new Duration(42L);
        success = true;
        RemoteDependencyTelemetry dt = new RemoteDependencyTelemetry(dependencyName, commandName, duration, success);
        dt.getContext().setInstrumentationKey(iKey);
        testCases.add(new Object[]{
                "Dependency, simple",
                dt,
                getTelemetryBuilder(RemoteDependencyTelemetry.ENVELOPE_NAME, iKey)
                        .setDependency(Dependency.newBuilder()
                                .setVer(dt.getVer())
                                .setData(commandName)
                                .setName(dependencyName)
                                .setDuration(LocalForwarderModelTransformer.transformDuration(duration))
                                .setSuccess(BoolValue.of(success))
                ).build()
        });

        dependencyName = "fake-dependency-name, II";
        commandName = "commander name, IV";
        duration = new Duration(240L);
        success = false;
        String target = "bullseye";
        double samplingPercent = 0.997;
        String resultCode = "611";
        String depType = "Fake";
        dt = new RemoteDependencyTelemetry(dependencyName, commandName, duration, success);
        dt.setTarget(target);
        dt.setSamplingPercentage(Double.valueOf(samplingPercent));
        dt.setResultCode(resultCode);
        dt.setType(depType);
        dt.getContext().setInstrumentationKey(iKey);
        dt.getProperties().putAll(props);
        dt.getMetrics().putAll(metrics);
        testCases.add(new Object[]{
                "Dependency, dense with props & metrics",
                dt,
                getTelemetryBuilder(RemoteDependencyTelemetry.ENVELOPE_NAME, iKey)
                        .setSamplingRate(DoubleValue.of(samplingPercent))
                        .setDependency(Dependency.newBuilder()
                                .setVer(dt.getVer())
                                .setData(commandName)
                                .setName(dependencyName)
                                .setDuration(LocalForwarderModelTransformer.transformDuration(duration))
                                .setSuccess(BoolValue.of(success))
                                .setTarget(target)
                                .setResultCode(resultCode)
                                .setType(depType)
                                .putAllProperties(props)
                                .putAllMeasurements(metrics)
                        ).build()
        });

        // Exception
        ExceptionTelemetry xt = new ExceptionTelemetry(new RuntimeException("ex-parent", new RuntimeException("ex-cause")));
        xt.getContext().setInstrumentationKey(iKey);
        xt.getProperties().putAll(props);
        xt.getMetrics().putAll(metrics);
        testCases.add(new Object[]{
                "Exception, with props & metrics",
                xt,
                getTelemetryBuilder(ExceptionTelemetry.ENVELOPE_NAME, iKey)
                        .setException(Exception.newBuilder()
                                .setVer(xt.getVer())
                                .addAllExceptions(buildExceptionDetails(xt))
                                .putAllProperties(props)
                                .putAllMeasurements(metrics)
                            )
                        .build()
        });

        // PerfCounter (deprecated)
        String categoryName = "some-category";
        String counterName = "some-counter";
        String instanceName = "some-instance";
        double value = 45.654;
        PerformanceCounterTelemetry pc = new PerformanceCounterTelemetry(categoryName, counterName, instanceName, value);
        pc.getContext().setInstrumentationKey(iKey);
        pc.getProperties().putAll(props);
        testCases.add(new Object[]{
                "PerformanceCounter, transformed to a metric",
                pc,
                getTelemetryBuilder(MetricTelemetry.ENVELOPE_NAME, iKey)
                    .setMetric(Metric.newBuilder()
                        .setVer(pc.getVer())
                        .addMetrics(DataPoint.newBuilder()
                                .setValue(value)
                                .setName(String.format("%s - %s", categoryName, counterName))
                                .setKind(DataPointType.Measurement)
                        )
                        .putAllProperties(props)
                        .putProperties("CustomPerfCounter", "true")
                        .putProperties("CounterInstanceName", instanceName)
                ).build()
        });

        return testCases;
    }

    private static Iterable<? extends ExceptionDetails> buildExceptionDetails(final ExceptionTelemetry et) {
        Collection<ExceptionDetails> details = new ArrayList<>();
        List<com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails> exceptions = et.getExceptions();
        for (com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails ed : exceptions) {
            ExceptionDetails.Builder edb = ExceptionDetails.newBuilder()
                    .setHasFullStack(BoolValue.of(ed.getHasFullStack()))
                    .setId(ed.getId())
                    .setOuterId(ed.getOuterId());
            if (ed.getMessage() != null) edb.setMessage(ed.getMessage());
            if (ed.getTypeName() != null) edb.setTypeName(ed.getTypeName());
            if (ed.getStack() != null) edb.setStack(ed.getStack());
            if (ed.getParsedStack() != null) {
                for (com.microsoft.applicationinsights.internal.schemav2.StackFrame sf : ed.getParsedStack()) {
                    StackFrame.Builder sfb = StackFrame.newBuilder()
                            .setLine(sf.getLine())
                            .setMethod(sf.getMethod())
                            .setLevel(sf.getLevel());
                    if (sf.getFileName() != null) sfb.setFileName(sf.getFileName());
                    if (sf.getAssembly() != null) sfb.setAssembly(sf.getAssembly());
                    edb.addParsedStack(sfb);
                }
            }
            details.add(edb.build());
        }
        return details;
    }

    @Parameter(0) public String testName;
    @Parameter(1) public BaseTelemetry<?> internalModel;
    @Parameter(2) public Telemetry grpcModel;

    @Test
    public void transformBaseTelemetryToProtobufModel() {
        System.out.printf("Running test '%s'%n", testName);
        assertEquals(grpcModel, LocalForwarderModelTransformer.transform(internalModel));
    }

}
