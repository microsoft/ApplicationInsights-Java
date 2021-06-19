package com.microsoft.applicationinsights;

import com.azure.monitor.opentelemetry.exporter.implementation.models.*;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opentelemetry.api.trace.TraceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

// naming convention:
// * MonitorDomain data
// * TelemetryItem telemetry
public class TelemetryUtil {

    public static TelemetryItem createMetricsTelemetry(TelemetryClient telemetryClient, String name, double value) {
        TelemetryItem telemetry = new TelemetryItem();
        MetricsData data = new MetricsData();
        MetricDataPoint point = new MetricDataPoint();
        telemetryClient.initMetricTelemetry(telemetry, data, point);

        point.setName(name);
        point.setValue(value);
        point.setDataPointType(DataPointType.MEASUREMENT);

        telemetry.setTime(FormattedTime.fromNow());

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

    // TODO (trask) can we move getProperties up to MonitorDomain, or if not, a common interface?
    public static Map<String, String> getProperties(MonitorDomain data) {
        if (data instanceof AvailabilityData) {
            AvailabilityData availabilityData = (AvailabilityData) data;
            Map<String, String> properties = availabilityData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                availabilityData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof MessageData) {
            MessageData messageData = (MessageData) data;
            Map<String, String> properties = messageData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                messageData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof MetricsData) {
            MetricsData metricsData = (MetricsData) data;
            Map<String, String> properties = metricsData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                metricsData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof PageViewData) {
            PageViewData pageViewData = (PageViewData) data;
            Map<String, String> properties = pageViewData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                pageViewData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof PageViewPerfData) {
            PageViewPerfData pageViewPerfData = (PageViewPerfData) data;
            Map<String, String> properties = pageViewPerfData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                pageViewPerfData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof RemoteDependencyData) {
            RemoteDependencyData remoteDependencyData = (RemoteDependencyData) data;
            Map<String, String> properties = remoteDependencyData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                remoteDependencyData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof RequestData) {
            RequestData requestData = (RequestData) data;
            Map<String, String> properties = requestData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                requestData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof TelemetryEventData) {
            TelemetryEventData eventData = (TelemetryEventData) data;
            Map<String, String> properties = eventData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                eventData.setProperties(properties);
            }
            return properties;
        } else if (data instanceof TelemetryExceptionData) {
            TelemetryExceptionData exceptionData = (TelemetryExceptionData) data;
            Map<String, String> properties = exceptionData.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
                exceptionData.setProperties(properties);
            }
            return properties;
        } else {
            throw new IllegalArgumentException("Unexpected type: " + data.getClass().getName());
        }
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
        } else if (data instanceof TelemetryEventData) { // TODO (trask) can we rename to EventData to match above?
            return "EventData";
        } else if (data instanceof TelemetryExceptionData) { // TODO (trask) can we rename to ExceptionData to match above?
            return "ExceptionData";
        } else {
            throw new IllegalArgumentException("Unexpected type: " + data.getClass().getName());
        }
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
