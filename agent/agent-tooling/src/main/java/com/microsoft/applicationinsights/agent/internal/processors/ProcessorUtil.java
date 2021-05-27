package com.microsoft.applicationinsights.agent.internal.processors;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;

public class ProcessorUtil {
    private static final AttributeKey<Boolean> AI_LOG_KEY = AttributeKey.booleanKey("applicationinsights.internal.log");
    public static boolean isSpanOfTypeLog(SpanData span) {
        Boolean isLog = span.getAttributes().get(AI_LOG_KEY);
        return isLog != null && isLog;
    }
}
