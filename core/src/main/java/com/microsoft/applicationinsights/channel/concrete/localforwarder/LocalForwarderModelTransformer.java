package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Int32Value;
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
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
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
import com.microsoft.localforwarder.library.inputs.contracts.SeverityLevel;
import com.microsoft.localforwarder.library.inputs.contracts.StackFrame;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry.Builder;

import java.util.HashMap;
import java.util.Map;

final class LocalForwarderModelTransformer {

    private LocalForwarderModelTransformer(){}

    private static final Map<String, Function<BaseTelemetry, Telemetry>> transformers = new HashMap<>();

    private static final int CURRENT_ENVELOPE_VERSION = 1;

    @VisibleForTesting
    static final Function<com.microsoft.applicationinsights.internal.schemav2.StackFrame, StackFrame> STACK_FRAME_TRANSFORMER_FUNCTION = new Function<com.microsoft.applicationinsights.internal.schemav2.StackFrame, StackFrame>() {
        @Override
        public StackFrame apply(com.microsoft.applicationinsights.internal.schemav2.StackFrame s) {
            final StackFrame.Builder sfb = StackFrame.newBuilder()
                    .setLevel(s.getLevel())
                    .setLine(s.getLine());
            if (s.getMethod() != null) sfb.setMethod(s.getMethod());
            if (s.getAssembly() != null) sfb.setAssembly(s.getAssembly());
            if (s.getFileName() != null) sfb.setFileName(s.getFileName());

            return sfb.build();
        }
    };

    @VisibleForTesting
    static final Function<com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails, ExceptionDetails> EXCEPTION_DETAILS_TRANSFORMER_FUNCTION = new Function<com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails, ExceptionDetails>() {
        @Override
        public ExceptionDetails apply(com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails d) {
            final ExceptionDetails.Builder edb = ExceptionDetails.newBuilder()
                    .setId(d.getId())
                    .setOuterId(d.getOuterId())
                    .setHasFullStack(BoolValue.of(d.getHasFullStack()));

            if (d.getTypeName() != null) edb.setTypeName(d.getTypeName());
            if (d.getMessage() != null) edb.setMessage(d.getMessage());
            if (d.getStack() != null) edb.setStack(d.getStack());
            if (d.getParsedStack() != null) edb.addAllParsedStack(Iterables.transform(d.getParsedStack(), STACK_FRAME_TRANSFORMER_FUNCTION));

            return edb.build();
        }
    };


    @VisibleForTesting
    static SeverityLevel transformSeverityLevel(com.microsoft.applicationinsights.telemetry.SeverityLevel input) {
        if (input == null) {
            return SeverityLevel.UNRECOGNIZED;
        }
        switch (input) {
            case Verbose: return SeverityLevel.Verbose;
            case Information: return SeverityLevel.Information;
            case Error: return SeverityLevel.Error;
            case Critical: return SeverityLevel.Critical;
            case Warning: return SeverityLevel.Warning;
        }
        return SeverityLevel.UNRECOGNIZED;
    }

    @VisibleForTesting
    static DataPointType transformDataPointType(com.microsoft.applicationinsights.internal.schemav2.DataPointType input) {
        if (input == null) {
            return DataPointType.UNRECOGNIZED;
        }
        switch (input) {
            case Aggregation: return DataPointType.Aggregation;
            case Measurement: return DataPointType.Measurement;
        }
        return DataPointType.UNRECOGNIZED;
    }

    @VisibleForTesting
    static <T extends BaseTelemetry> Telemetry.Builder telemetryBuilderWithStandardFields(T telemetry) {
        Preconditions.checkArgument(telemetry.getContext() != null, "TelemetryContext is null for telemetry with "+telemetry.getBaseTypeName());
        TelemetryContext context = telemetry.getContext();
        final String iKey = context.getInstrumentationKey();
        Preconditions.checkArgument(iKey != null, "The TelemetryContext.InstrumentationKey is null inside "+telemetry.getBaseTypeName());

        final Builder tb = Telemetry.newBuilder();
        if (telemetry.getTimestamp() != null) tb.setDateTime(LocalStringsUtils.getDateFormatter().format(telemetry.getTimestamp()));
        if (telemetry.getSequence() != null) tb.setSequenceNumber(telemetry.getSequence());
        if (telemetry instanceof BaseSampleSourceTelemetry) {
            final BaseSampleSourceTelemetry bsst = (BaseSampleSourceTelemetry) telemetry;
            if (bsst.getSamplingPercentage() != null) tb.setSamplingRate(DoubleValue.of(bsst.getSamplingPercentage()));
        }
        tb.setInstrumentationKey(iKey);
        if (context.getTags() != null) tb.putAllTags(context.getTags());
        tb.setVer(CURRENT_ENVELOPE_VERSION);

        final String envelopName;
        if (telemetry instanceof PerformanceCounterTelemetry) {
            envelopName = MetricTelemetry.ENVELOPE_NAME;
        } else {
            envelopName = telemetry.getEnvelopName();
        }
        tb.setDataTypeName(generateDataTypeName(iKey, envelopName));
        return tb;
    }

    @VisibleForTesting
    static String generateDataTypeName(String iKey, String envelopName) {
        return BaseTelemetry.getTelemetryName(BaseTelemetry.normalizeInstrumentationKey(iKey), envelopName);
    }

    @VisibleForTesting
    static Duration transformDuration(com.microsoft.applicationinsights.telemetry.Duration d) {
        Preconditions.checkNotNull(d);
        // magic numbers: 60s=1min, 3600s=1hr, 86400s=60*60*24=1day, 10^6ns=1ms
        return Duration.newBuilder()
                .setSeconds(d.getSeconds() + d.getMinutes()*60 + d.getHours()*3600 + d.getDays()*86400)
                .setNanos(d.getMilliseconds() * 1_000_000)
                .build();
    }

    static {
        // Trace
        transformers.put(TraceTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry>() {
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                TraceTelemetry t = (TraceTelemetry) bt;

                final Message.Builder mb = Message.newBuilder()
                        .setVer(t.getVer());

                final SeverityLevel sl = transformSeverityLevel(t.getSeverityLevel());

                if (sl != SeverityLevel.UNRECOGNIZED) mb.setSeverityLevel(sl);
                if (t.getMessage() != null) mb.setMessage(t.getMessage());
                if (t.getProperties() != null) mb.putAllProperties(t.getProperties());

                return telemetryBuilderWithStandardFields(t).setMessage(mb).build();
            }
        });
        // Metric
        transformers.put(MetricTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry>() {
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                MetricTelemetry t = (MetricTelemetry) bt;

                final Metric.Builder mb = Metric.newBuilder()
                        .setVer(t.getVer());
                if (t.getProperties() != null) mb.putAllProperties(t.getProperties());

                DataPoint.Builder dpb = DataPoint.newBuilder()
                        .setValue(t.getValue());

                final DataPointType dpType = transformDataPointType(t.getKind());

                if (dpType != DataPointType.UNRECOGNIZED) dpb.setKind(dpType);
                if (t.getCount() != null) dpb.setCount(Int32Value.of(t.getCount()));
                if (t.getMin() != null) dpb.setMin(DoubleValue.of(t.getMin()));
                if (t.getMax() != null) dpb.setMax(DoubleValue.of(t.getMax()));
                if (t.getStandardDeviation() != null) dpb.setStdDev(DoubleValue.of(t.getStandardDeviation()));
                if (t.getName() != null) dpb.setName(t.getName());

                mb.addMetrics(dpb);

                return telemetryBuilderWithStandardFields(t).setMetric(mb).build();
            }
        });
        // PerformanceCounter
        transformers.put(PerformanceCounterTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry>() {
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                PerformanceCounterTelemetry t = (PerformanceCounterTelemetry) bt;
                final Metric.Builder mb = Metric.newBuilder()
                        .setVer(t.getVer());
                if (t.getProperties() != null) mb.putAllProperties(t.getProperties());

                String metricName = null;
                if (t.getCategoryName() != null && t.getCounterName() != null) {
                    metricName = String.format("%s - %s", t.getCategoryName(), t.getCounterName());
                } else if (t.getCategoryName() != null) {
                    metricName = t.getCategoryName();
                } else if (t.getCounterName() != null) {
                    metricName = t.getCounterName();
                }

                final DataPoint.Builder dpb = DataPoint.newBuilder()
                        .setKind(DataPointType.Measurement)
                        .setValue(t.getValue());
                if (metricName != null) dpb.setName(metricName);

                mb.addMetrics(dpb);

                mb.putProperties("CustomPerfCounter", "true");
                if (t.getInstanceName() != null) mb.putProperties("CounterInstanceName", t.getInstanceName());

                return telemetryBuilderWithStandardFields(t).setMetric(mb).build();
            }
        });
        // Dependency
        transformers.put(RemoteDependencyTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry>() {
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                RemoteDependencyTelemetry t = (RemoteDependencyTelemetry) bt;

                final Dependency.Builder db = Dependency.newBuilder()
                        .setVer(t.getVer())
                        .setSuccess(BoolValue.of(t.getSuccess()))
                        .setDuration(transformDuration(t.getDuration()));

                if (t.getProperties() != null) db.putAllProperties(t.getProperties());
                if (t.getName() != null) db.setName(t.getName());
                if (t.getId() != null) db.setId(t.getId());
                if (t.getResultCode() != null) db.setResultCode(t.getResultCode());
                if (t.getCommandName() != null) db.setData(t.getCommandName());
                if (t.getType() != null) db.setType(t.getType());
                if (t.getTarget() != null) db.setTarget(t.getTarget());
                if (t.getMetrics() != null) db.putAllMeasurements(t.getMetrics());

                return telemetryBuilderWithStandardFields(t).setDependency(db).build();
            }
        });
        // Event
        transformers.put(EventTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry>() {
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                EventTelemetry t = (EventTelemetry) bt;

                final Event.Builder eb = Event.newBuilder()
                        .setVer(t.getVer());

                if (t.getName() != null) eb.setName(t.getName());
                if (t.getProperties() != null) eb.putAllProperties(t.getProperties());
                if (t.getMetrics() != null) eb.putAllMeasurements(t.getMetrics());

                return telemetryBuilderWithStandardFields(t).setEvent(eb).build();
            }
        });
        // Exception
        transformers.put(ExceptionTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry>(){
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                ExceptionTelemetry t = (ExceptionTelemetry) bt;

                final Exception.Builder eb = Exception.newBuilder()
                        .setVer(t.getVer());
                final SeverityLevel sl = transformSeverityLevel(t.getSeverityLevel());

                if (sl != SeverityLevel.UNRECOGNIZED) eb.setSeverityLevel(sl);
                if (t.getProblemId() != null) eb.setProblemId(t.getProblemId());
                if (t.getProperties() != null) eb.putAllProperties(t.getProperties());
                if (t.getMetrics() != null) eb.putAllMeasurements(t.getMetrics());
                if (t.getExceptions() != null) eb.addAllExceptions(Iterables.transform(t.getExceptions(), EXCEPTION_DETAILS_TRANSFORMER_FUNCTION));

                return telemetryBuilderWithStandardFields(t).setException(eb).build();
            }
        });
        // PageView
        transformers.put(PageViewTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry>(){
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                PageViewTelemetry t = (PageViewTelemetry) bt;

                final Event.Builder eb = Event.newBuilder()
                        .setVer(t.getVer());
                if (t.getName() != null) eb.setName(t.getName());
                if (t.getProperties() != null) eb.putAllProperties(t.getProperties());
                if (t.getMetrics() != null) eb.putAllMeasurements(t.getMetrics());

                final PageView.Builder pvb = PageView.newBuilder()
                        .setEvent(eb);
                if (t.getUrlString() != null) pvb.setUrl(t.getUrlString());
                if (t.getDurationObject() != null) pvb.setDuration(transformDuration(t.getDurationObject()));

                return telemetryBuilderWithStandardFields(t).setPageView(pvb).build();
            }
        });
        // Request
        transformers.put(RequestTelemetry.BASE_TYPE, new Function<BaseTelemetry, Telemetry> () {
            @Override
            public Telemetry apply(BaseTelemetry bt) {
                Preconditions.checkNotNull(bt);
                RequestTelemetry t = (RequestTelemetry) bt;

                final Request.Builder rb = Request.newBuilder()
                        .setVer(t.getVer())
                        .setDuration(transformDuration(t.getDuration()))
                        .setSuccess(BoolValue.of(t.isSuccess()));
                if (t.getId() != null) rb.setId(t.getId());
                if (t.getResponseCode() != null) rb.setResponseCode(t.getResponseCode());
                if (t.getSource() != null) rb.setSource(t.getSource());
                if (t.getName() != null) rb.setName(t.getName());
                if (t.getUrlString() != null) rb.setUrl(t.getUrlString());
                if (t.getProperties() != null) rb.putAllProperties(t.getProperties());
                if (t.getMetrics() != null) rb.putAllMeasurements(t.getMetrics());

                return telemetryBuilderWithStandardFields(t).setRequest(rb).build();
            }
        });
    }

    /**
     * Uses BaseTelemetry.getBaseTypeName() to find the appropriate function to transform the "internal model" to the protobuf models.
     *
     * @param internalModel the BaseTelemetry to transform
     * @return the given BaseTelemetry as a Telemetry object, or null if no transformer could be found for the given type.
     * @throws NullPointerException if internalModel is null
     */
    public static Telemetry transform(BaseTelemetry<?> internalModel) {
        final Function<BaseTelemetry, Telemetry> transformer = transformers.get(internalModel.getBaseTypeName());
        if (transformer == null) {
            return null;
        }
        return transformer.apply(internalModel);
    }
}
