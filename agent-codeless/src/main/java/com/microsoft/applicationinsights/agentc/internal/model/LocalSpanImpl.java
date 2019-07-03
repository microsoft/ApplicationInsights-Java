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

package com.microsoft.applicationinsights.agentc.internal.model;

import java.util.Date;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.MessageSupplier;
import org.glowroot.instrumentation.api.Setter;
import org.glowroot.instrumentation.api.Span;
import org.glowroot.instrumentation.api.Timer;
import org.glowroot.instrumentation.engine.impl.NopTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalSpanImpl implements Span {

    static final String PREFIX = "__custom,";

    private static final Logger logger = LoggerFactory.getLogger(LocalSpanImpl.class);

    private final String operationId;
    private final String operationParentId;

    private final String text;
    private final long startTimeMillis;
    private final MessageSupplier messageSupplier;

    private volatile @MonotonicNonNull Throwable exception;

    LocalSpanImpl(String operationId, String operationParentId, String text, long startTimeMillis,
                  MessageSupplier messageSupplier) {
        this.operationId = operationId;
        this.operationParentId = operationParentId;
        this.text = text;
        this.startTimeMillis = startTimeMillis;
        this.messageSupplier = messageSupplier;
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
    public Timer extend() {
        // timers are not used by ApplicationInsights
        return NopTransactionService.TIMER;
    }

    @Override
    public Object getMessageSupplier() {
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

        long endTimeMillis = System.currentTimeMillis();
        long durationMillis = endTimeMillis - startTimeMillis;

        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();
        telemetry.getContext().getOperation().setId(operationId);
        telemetry.getContext().getOperation().setParentId(operationParentId);
        telemetry.setName(getTelemetryName(text));
        telemetry.setType("OTHER");
        telemetry.setDuration(new Duration(durationMillis));
        telemetry.setSuccess(exception == null);

        TelemetryClient telemetryClient = Global.getTelemetryClient();
        telemetryClient.track(telemetry);
        if (exception != null) {
            ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);
            exceptionTelemetry.getContext().getOperation().setId(operationId);
            exceptionTelemetry.getContext().getOperation().setParentId(operationParentId);
            exceptionTelemetry.setTimestamp(new Date(endTimeMillis));

            telemetryClient.track(exceptionTelemetry);
        }
    }

    @VisibleForTesting
    static @Nullable String getTelemetryName(String text) {

        int startIndex = PREFIX.length();
        int index = text.indexOf(',', startIndex);
        if (index == -1) {
            logger.warn("unexpected local span message: {}", text);
            return text;
        }
        String className = text.substring(startIndex, index);
        String methodName = text.substring(index + 1);

        return className.replace('.', '/') + '.' + methodName;
    }
}
