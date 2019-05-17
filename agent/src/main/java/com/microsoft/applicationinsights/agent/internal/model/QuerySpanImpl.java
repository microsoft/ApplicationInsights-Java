package com.microsoft.applicationinsights.agent.internal.model;

import java.util.concurrent.TimeUnit;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QuerySpan;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.Timer;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Telemetry;

public class QuerySpanImpl implements QuerySpan {

    private final String type;
    private final long startTimeMillis;
    private final String queryText;
    private final QueryMessageSupplier messageSupplier;

    private final TelemetryClient client;

    public QuerySpanImpl(String type, long startTimeMillis, String queryText, QueryMessageSupplier messageSupplier,
                         TelemetryClient client) {
        this.type = type;
        this.startTimeMillis = startTimeMillis;
        this.queryText = queryText;
        this.messageSupplier = messageSupplier;
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
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        endInternal();
    }

    @Override
    public void endWithError(Throwable t) {
        endInternal();
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
        // sender.send(toTelemetry(System.nanoTime()));
    }

    private Telemetry toTelemetry(long endNanoTime) {
        // TODO Auto-generated method stub
        return null;
    }
}
