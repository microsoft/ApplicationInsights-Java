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

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge.ExceptionTelemetry;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge.RemoteDependencyTelemetry;
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

    private final SdkBridge sdkBridge;

    private final String text;
    private final long startTimeMillis;
    private final MessageSupplier messageSupplier;

    private volatile @MonotonicNonNull Throwable exception;

    LocalSpanImpl(SdkBridge sdkBridge, String text, long startTimeMillis, MessageSupplier messageSupplier) {
        this.sdkBridge = sdkBridge;
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
        RemoteDependencyTelemetry telemetry = createRemoteDependencyTelemetry(text, startTimeMillis, exception);
        if (telemetry == null) {
            return;
        }
        sdkBridge.track(telemetry);
        if (exception != null) {
            sdkBridge.track(new ExceptionTelemetry(exception, telemetry.getSdkName()));
        }
    }

    @VisibleForTesting
    static @Nullable RemoteDependencyTelemetry createRemoteDependencyTelemetry(String text, long startTimeMillis,
                                                                               @Nullable Throwable exception) {
        int startIndex = PREFIX.length();
        int index = text.indexOf(',', startIndex);
        if (index == -1) {
            logger.warn("unexpected local span message: {}", text);
            return null;
        }
        String className = text.substring(startIndex, index);

        startIndex = index + 1;
        index = text.indexOf(',', startIndex);
        if (index == -1) {
            logger.warn("unexpected local span message: {}", text);
            return null;
        }
        String methodName = text.substring(startIndex, index);

        startIndex = index + 1;
        index = text.indexOf(',', startIndex);
        if (index == -1) {
            logger.warn("unexpected local span message: {}", text);
            return null;
        }
        long thresholdInMS;
        try {
            thresholdInMS = Long.parseLong(text.substring(startIndex, index));
        } catch (NumberFormatException e) {
            logger.error("unexpected local span message: {}", text);
            return null;
        }
        String classType = text.substring(index + 1);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        if (durationMillis < thresholdInMS) {
            return null;
        }
        RemoteDependencyTelemetry telemetry =
                new RemoteDependencyTelemetry(startTimeMillis, durationMillis, classType, exception == null,
                        "ja-custom");
        telemetry.setName(className.replace('.', '/') + '.' + methodName);
        return telemetry;
    }
}
