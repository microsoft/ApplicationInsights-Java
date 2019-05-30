package com.microsoft.applicationinsights.agent.internal.model;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.*;

import java.util.Date;
import java.util.Map;

public class QuerySpanImpl implements QuerySpan {

    private final String type;
    private final String dest;
    private final String text;
    private final QueryMessageSupplier messageSupplier;
    private final long startTimeMillis;

    private final TelemetryClient client;

    private volatile @Nullable Throwable exception;

    private volatile long totalMillis = -1;

    QuerySpanImpl(String type, String dest, String text, QueryMessageSupplier messageSupplier, long startTimeMillis,
                  TelemetryClient client) {
        this.type = type;
        this.dest = dest;
        this.text = text;
        this.messageSupplier = messageSupplier;
        this.startTimeMillis = startTimeMillis;
        this.client = client;
    }

    @Override
    public void rowNavigationAttempted() {
        // not capturing query rows since query is flushed to collector when complete
    }

    @Override
    public void incrementCurrRow() {
        // not capturing query rows since query is flushed to collector when complete
    }

    @Override
    public void setCurrRow(long row) {
        // not capturing query rows since query is flushed to collector when complete
    }

    @Override
    public void end() {
        endInternal();
    }

    @Override
    public void endWithLocationStackTrace(long thresholdNanos) {
        endInternal();
    }

    @Override
    public void endWithError(Throwable t) {
        exception = t;
        endInternal();
    }

    @Override
    public long partOneEnd() {
        return endInternalPart1();
    }

    @Override
    public long partOneEndWithLocationStackTrace(long thresholdNanos) {
        return endInternalPart1();
    }

    @Override
    public void partTwoEnd() {
        endInternalPart2();
    }

    @Override
    public Timer extend() {
        // xyzzy timers are not used by ApplicationInsights
        return NopTransactionService.TIMER;
    }

    @Override
    public QueryMessageSupplier getMessageSupplier() {
        return messageSupplier;
    }

    @Override
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {
    }

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {
    }

    private void endInternal() {
        endInternalPart1();
        endInternalPart2();
    }

    private long endInternalPart1() {
        totalMillis = System.currentTimeMillis() - startTimeMillis;
        return totalMillis;
    }

    private void endInternalPart2() {
        if (!type.equals("SQL")) {
            return;
        }
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();
        telemetry.setTimestamp(new Date(startTimeMillis));
        telemetry.setDuration(new Duration(totalMillis));
        telemetry.setName(dest);
        telemetry.setCommandName(text);
        telemetry.setSuccess(exception == null);
        telemetry.setType(type);

        Map<String, ?> detail = messageSupplier.get();
        Integer batchCount = (Integer) detail.get("batchCount");
        if (batchCount != null) {
            telemetry.getProperties().put("Args", " [Batch of " + batchCount + "]");
        }
        Boolean batchStatement = (Boolean) detail.get("batchStatement");
        if (batchStatement != null) {
            telemetry.getProperties().put("Args", " [Batch]");
        }
        Object explainPlan = detail.get("explainPlan");
        if (explainPlan instanceof String) {
            telemetry.getProperties().put("Query Plan", (String) explainPlan);
        } else if (explainPlan instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) explainPlan;
            StringBuilder sb = new StringBuilder();
            boolean needsSeparator = false;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (needsSeparator) {
                    sb.append(',');
                }
                sb.append(entry.getKey());
                sb.append(':');
                sb.append(entry.getValue());
                needsSeparator = true;
            }
            telemetry.getProperties().put("Query Plan", sb.toString());
        }

        client.track(telemetry);
        if (exception != null) {
            ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);
            client.track(exceptionTelemetry);
        }
    }
}
