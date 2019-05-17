package com.microsoft.applicationinsights.agent3.model;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncQuerySpan;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Timer;

import com.microsoft.applicationinsights.TelemetryClient;

public class AsyncQuerySpanImpl extends QuerySpanImpl implements AsyncQuerySpan {

    public AsyncQuerySpanImpl(String type, long startTimeMillis, String queryText, QueryMessageSupplier messageSupplier,
                              TelemetryClient client) {
        super(type, startTimeMillis, queryText, messageSupplier, client);
    }

    @Override
    public void stopSyncTimer() {
        // xyzzy timers are not used by ApplicationInsights
    }

    @Override
    public Timer extendSyncTimer() {
        // xyzzy timers are not used by ApplicationInsights
        return NopTransactionService.TIMER;
    }
}
