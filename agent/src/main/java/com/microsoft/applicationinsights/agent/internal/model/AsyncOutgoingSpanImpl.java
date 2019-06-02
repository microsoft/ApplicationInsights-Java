package com.microsoft.applicationinsights.agent.internal.model;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncSpan;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Timer;

class AsyncOutgoingSpanImpl extends OutgoingSpanImpl implements AsyncSpan {

    public AsyncOutgoingSpanImpl(String type, String text, long startTimeMillis, String outgoingSpanId,
                                 MessageSupplier messageSupplier) {
        super(type, text, startTimeMillis, outgoingSpanId, messageSupplier);
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
