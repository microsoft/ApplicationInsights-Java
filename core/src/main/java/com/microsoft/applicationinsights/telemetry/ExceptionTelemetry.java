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

package com.microsoft.applicationinsights.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.StackFrame;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Telemetry type used to track exceptions.
 */
public final class ExceptionTelemetry extends BaseTelemetry<ExceptionData> implements SupportSampling {
    private Double samplingPercentage;
    private final ExceptionData data;
    private Throwable throwable;

    private ExceptionTelemetry() {
        super();
        data = new ExceptionData();
        initialize(data.getProperties());
        setExceptionHandledAt(ExceptionHandledAt.Unhandled);
    }

    /**
     * Initializes a new instance.
     * @param stackSize The max stack size to report.
     * @param exception The exception to track.
     */
    public ExceptionTelemetry(Throwable exception, int stackSize) {
        this();
        setException(exception, stackSize);
    }

    /**
     * Initializes a new instance.
     * @param exception The exception to track.
     */
    public ExceptionTelemetry(Throwable exception) {
        this(exception, Integer.MAX_VALUE);
    }

    public Exception getException() {
        return throwable instanceof Exception ? (Exception)throwable : null;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setException(Throwable throwable) {
        setException(throwable, Integer.MAX_VALUE);
    }

    public void setException(Throwable throwable, int stackSize) {
        this.throwable = throwable;
        updateException(throwable, stackSize);
    }

    /**
     * Gets the value indicated where the exception was handled.
     * @return The value indicating the exception
     */
    public ExceptionHandledAt getExceptionHandledAt() {
        return Enum.valueOf(ExceptionHandledAt.class, data.getHandledAt());
    }

    /**
     * Sets the value indicated where the exception was handled.
     * @param value The value indicating the exception
     */
    public void setExceptionHandledAt(ExceptionHandledAt value) {
        data.setHandledAt(value.toString());
    }

    /**
     * Gets a map of application-defined exception metrics.
     * @return The map of metrics
     */
    public ConcurrentMap<String,Double> getMetrics() {
        return data.getMeasurements();
    }

    public void setSeverityLevel(SeverityLevel severityLevel) {
        data.setSeverityLevel(severityLevel);
    }

    public SeverityLevel getSeverityLevel() {
        return data.getSeverityLevel();
    }

    @Override
    public Double getSamplingPercentage() {
        return samplingPercentage;
    }

    @Override
    public void setSamplingPercentage(Double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
    }

    @Override
    protected void additionalSanitize() {
        Sanitizer.sanitizeMeasurements(this.getMetrics());
    }

    @Override
    protected ExceptionData getData() {
        return data;
    }

    protected List<ExceptionDetails> getExceptions() {
        return data.getExceptions();
    }

    private void updateException(Throwable throwable, int stackSize) {
        ArrayList<ExceptionDetails> exceptions = new ArrayList<ExceptionDetails>();
        convertExceptionTree(throwable, null, exceptions, stackSize);

        data.setExceptions(exceptions);
    }

    private static void convertExceptionTree(Throwable exception, ExceptionDetails parentExceptionDetails, List<ExceptionDetails> exceptions, int stackSize) {
        if (exception == null) {
            exception = new Exception("");
        }

        if (stackSize == 0) {
            return;
        }

        ExceptionDetails exceptionDetails = createWithStackInfo(exception, parentExceptionDetails);
        exceptions.add(exceptionDetails);

        if (exception.getCause() != null) {
            convertExceptionTree(exception.getCause(), exceptionDetails, exceptions, stackSize - 1);
        }
    }

    private static ExceptionDetails createWithStackInfo(Throwable exception, ExceptionDetails parentExceptionDetails) {
        if (exception == null) {
            throw new IllegalArgumentException("exception cannot be null");
        }

        ExceptionDetails exceptionDetails = new ExceptionDetails();
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
            List<StackFrame> stack = exceptionDetails.getParsedStack();

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

            exceptionDetails.setHasFullStack(true); // TODO: sanitize and trim exception stack trace.
        }

        return exceptionDetails;
    }
}
