package com.microsoft.applicationinsights;

import com.azure.monitor.opentelemetry.exporter.implementation.models.*;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opentelemetry.api.trace.TraceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.*;

// naming convention:
// * MonitorDomain data
// * TelemetryItem telemetry
public class TelemetryUtil {

    public static TelemetryItem createMetricsTelemetry(String name, double value) {
        return createTelemetry(createMetricsData(name, value));
    }

    public static MetricsData createMetricsData(String name, double value) {
        MetricDataPoint point = new MetricDataPoint();
        point.setName(name);
        point.setValue(value);

        MetricsData data = new MetricsData();
        data.setMetrics(Collections.singletonList(point));
        return data;
    }

    public static TelemetryItem createExceptionTelemetry(Exception exception) {
        TelemetryExceptionData data = new TelemetryExceptionData();
        data.setExceptions(getExceptions(exception));
        return createTelemetry(data);
    }

    public static TelemetryEventData createEventData(String name) {
        TelemetryEventData data = new TelemetryEventData();
        data.setName(name);
        return data;
    }

    public static MessageData createMessageData(String message) {
        MessageData data = new MessageData();
        data.setMessage(message);
        return data;
    }

    public static TelemetryItem createMessageTelemetry(String message) {
        return createTelemetry(createMessageData(message));
    }

    public static TelemetryItem createRequestTelemetry(String name, Date timestamp, long duration, String responseCode, boolean success) {
        RequestData data = new RequestData();
        data.setName(name);
        data.setDuration(getFormattedDuration(duration));
        data.setResponseCode(responseCode);
        data.setSuccess(success);
        TelemetryItem telemetry = createTelemetry(data);
        telemetry.setTime(getFormattedTime(timestamp.getTime()));
        return telemetry;
    }

    public static TelemetryItem createRemoteDependencyTelemetry(String name, String command, long durationMillis, boolean success) {
        RemoteDependencyData data = new RemoteDependencyData();
        data.setName(name);
        data.setData(command);
        data.setDuration(getFormattedDuration(durationMillis));
        data.setSuccess(success);
        return createTelemetry(data);
    }

    public static TelemetryItem createTelemetry(MonitorDomain data) {
        MonitorBase base = new MonitorBase();
        base.setBaseData(data);
        base.setBaseType(getBaseType(data));

        TelemetryItem telemetry = new TelemetryItem();
        telemetry.setData(base);
        return telemetry;
    }

    public static List<TelemetryExceptionDetails> getExceptions(Throwable throwable) {
        List<TelemetryExceptionDetails> exceptions = new ArrayList<>();
        convertExceptionTree(throwable, null, exceptions, Integer.MAX_VALUE);
        return exceptions;
    }

    private static void convertExceptionTree(Throwable exception, TelemetryExceptionDetails parentExceptionDetails, List<TelemetryExceptionDetails> exceptions, int stackSize) {
        if (exception == null) {
            exception = new Exception("");
        }

        if (stackSize == 0) {
            return;
        }

        TelemetryExceptionDetails exceptionDetails = createWithStackInfo(exception, parentExceptionDetails);
        exceptions.add(exceptionDetails);

        if (exception.getCause() != null) {
            convertExceptionTree(exception.getCause(), exceptionDetails, exceptions, stackSize - 1);
        }
    }

    private static TelemetryExceptionDetails createWithStackInfo(Throwable exception, TelemetryExceptionDetails parentExceptionDetails) {
        if (exception == null) {
            throw new IllegalArgumentException("exception cannot be null");
        }

        TelemetryExceptionDetails exceptionDetails = new TelemetryExceptionDetails();
        exceptionDetails.setId(exception.hashCode());
        exceptionDetails.setTypeName(exception.getClass().getName());

        String exceptionMessage = exception.getMessage();
        if (Strings.isNullOrEmpty(exceptionMessage)) {
            exceptionMessage = exception.getClass().getName();
        }
        exceptionDetails.setMessage(exceptionMessage);

        if (parentExceptionDetails != null) {
            exceptionDetails.setOuterId(parentExceptionDetails.getId());
        }

        StackTraceElement[] trace = exception.getStackTrace();

        if (trace != null && trace.length > 0) {
            List<StackFrame> stack = new ArrayList<>();

            // We need to present the stack trace in reverse order.

            for (int idx = 0; idx < trace.length; idx++) {
                StackTraceElement elem = trace[idx];

                if (elem.isNativeMethod()) {
                    continue;
                }

                String className = elem.getClassName();

                StackFrame frame = new StackFrame();
                frame.setLevel(idx);
                frame.setFileName(elem.getFileName());
                frame.setLine(elem.getLineNumber());

                if (!Strings.isNullOrEmpty(className)) {
                    frame.setMethod(elem.getClassName() + "." + elem.getMethodName());
                }
                else {
                    frame.setMethod(elem.getMethodName());
                }

                stack.add(frame);
            }

            exceptionDetails.setParsedStack(stack);

            exceptionDetails.setHasFullStack(true); // TODO: sanitize and trim exception stack trace.
        }

        return exceptionDetails;
    }

    private static String getBaseType(MonitorDomain data) {
        if (data instanceof AvailabilityData) {
            return "AvailabilityData"; // TODO (trask) is this right?
        } else if (data instanceof MessageData) {
            return "MessageData";
        } else if (data instanceof MetricsData) {
            return "MetricData";
        } else if (data instanceof PageViewData) {
            return "PageViewData";
        } else if (data instanceof PageViewPerfData) {
            return "PageViewPerfData"; // TODO (trask) is this right?
        } else if (data instanceof RemoteDependencyData) {
            return "RemoteDependencyData";
        } else if (data instanceof RequestData) {
            return "RequestData";
        } else if (data instanceof TelemetryEventData) {
            return "EventData";
        } else if (data instanceof TelemetryExceptionData) {
            return "ExceptionData";
        } else {
            throw new IllegalArgumentException("Unexpected type: " + data.getClass().getName());
        }
    }

    public static String currentTime() {
        return getFormattedTime(System.currentTimeMillis());
    }

    // FIXME (trask) share below functions with exporter

    private static final long MILLISECONDS_PER_DAY = DAYS.toMillis(1);
    private static final long MILLISECONDS_PER_HOUR = HOURS.toMillis(1);
    private static final long MILLISECONDS_PER_MINUTE = MINUTES.toMillis(1);
    private static final long MILLISECONDS_PER_SECOND = SECONDS.toMillis(1);

    public static String getFormattedDuration(long durationMillis) {
        long remainingMillis = durationMillis;

        long days = remainingMillis / MILLISECONDS_PER_DAY;
        remainingMillis = remainingMillis % MILLISECONDS_PER_DAY;

        long hours = remainingMillis / MILLISECONDS_PER_HOUR;
        remainingMillis = remainingMillis % MILLISECONDS_PER_HOUR;

        long minutes = remainingMillis / MILLISECONDS_PER_MINUTE;
        remainingMillis = remainingMillis % MILLISECONDS_PER_MINUTE;

        long seconds = remainingMillis / MILLISECONDS_PER_SECOND;
        remainingMillis = remainingMillis % MILLISECONDS_PER_SECOND;

        return days + "." + hours + ":" + minutes + ":" + seconds + "." + remainingMillis + "000";
    }

    public static String getFormattedTime(long epochNanos) {
        return Instant.ofEpochMilli(NANOSECONDS.toMillis(epochNanos))
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    // FIXME (trask) share this remaining code with the exporter

    public static final String SAMPLING_PERCENTAGE_TRACE_STATE = "ai-internal-sp";

    private static final Cache<String, OptionalFloat> parsedSamplingPercentageCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .build();

    private static final AtomicBoolean alreadyLoggedSamplingPercentageMissing = new AtomicBoolean();
    private static final AtomicBoolean alreadyLoggedSamplingPercentageParseError = new AtomicBoolean();

    private static final Logger logger = LoggerFactory.getLogger(TelemetryUtil.class);

    public static float getSamplingPercentage(TraceState traceState, float defaultValue, boolean warnOnMissing) {
        String samplingPercentageStr = traceState.get(SAMPLING_PERCENTAGE_TRACE_STATE);
        if (samplingPercentageStr == null) {
            if (warnOnMissing && !alreadyLoggedSamplingPercentageMissing.getAndSet(true)) {
                // sampler should have set the trace state
                logger.warn("did not find sampling percentage in trace state: {}", traceState);
            }
            return defaultValue;
        }
        try {
            return parseSamplingPercentage(samplingPercentageStr).orElse(defaultValue);
        } catch (ExecutionException e) {
            // this shouldn't happen
            logger.debug(e.getMessage(), e);
            return defaultValue;
        }
    }

    private static OptionalFloat parseSamplingPercentage(String samplingPercentageStr) throws ExecutionException {
        return parsedSamplingPercentageCache.get(samplingPercentageStr, () -> {
            try {
                return OptionalFloat.of(Float.parseFloat(samplingPercentageStr));
            } catch (NumberFormatException e) {
                if (!alreadyLoggedSamplingPercentageParseError.getAndSet(true)) {
                    logger.warn("error parsing sampling percentage trace state: {}", samplingPercentageStr, e);
                }
                return OptionalFloat.empty();
            }
        });
    }

    private static class OptionalFloat {

        private static final OptionalFloat EMPTY = new OptionalFloat();

        private final boolean present;
        private final float value;

        private OptionalFloat() {
            this.present = false;
            this.value = Float.NaN;
        }

        private OptionalFloat(float value) {
            this.present = true;
            this.value = value;
        }

        public static OptionalFloat empty() {
            return EMPTY;
        }

        public static OptionalFloat of(float value) {
            return new OptionalFloat(value);
        }

        public float orElse(float other) {
            return present ? value : other;
        }

        public boolean isEmpty() {
            return !present;
        }
    }
}
