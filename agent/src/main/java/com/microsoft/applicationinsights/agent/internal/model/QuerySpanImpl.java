/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.model;

import java.util.Map;

import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge.ExceptionTelemetry;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge.RemoteDependencyTelemetry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.QueryMessageSupplier;
import org.glowroot.instrumentation.api.QuerySpan;
import org.glowroot.instrumentation.api.Setter;
import org.glowroot.instrumentation.api.Timer;
import org.glowroot.instrumentation.engine.impl.NopTransactionService;

public class QuerySpanImpl implements QuerySpan {

    private final SdkBridge sdkBridge;

    private final String type;
    private final String dest;
    private final String text;
    private final QueryMessageSupplier messageSupplier;
    private final long startTimeMillis;

    private volatile @MonotonicNonNull Throwable exception;

    private volatile long totalMillis = -1;

    QuerySpanImpl(SdkBridge sdkBridge, String type, String dest, String text, QueryMessageSupplier messageSupplier,
                  long startTimeMillis) {
        this.sdkBridge=sdkBridge;
        this.type = type;
        this.dest = dest;
        this.text = text;
        this.messageSupplier = messageSupplier;
        this.startTimeMillis = startTimeMillis;
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
        // timers are not used by ApplicationInsights
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
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry(startTimeMillis, totalMillis, type,
                exception == null);
        telemetry.setName(dest);
        telemetry.setCommandName(text);

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

        sdkBridge.track(telemetry);
        if (exception != null) {
            sdkBridge.track(new ExceptionTelemetry(exception));
        }
    }
}
