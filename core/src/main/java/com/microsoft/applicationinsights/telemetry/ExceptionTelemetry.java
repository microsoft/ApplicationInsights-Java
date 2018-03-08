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

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.StackFrame;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Telemetry type used to track exceptions sent to Azure Application Insights.
 */
public final class ExceptionTelemetry extends BaseSampleSourceTelemetry<ExceptionData> {
    private Double samplingPercentage;
    private final ExceptionData data;
    private Throwable throwable;

    /**
     * Envelope Name for this telemetry.
     */
    private static final String ENVELOPE_NAME = "Exception";


    /**
     * Base Type for this telemetry.
     */
    private static final String BASE_TYPE = "ExceptionData";

    /**
     * Maximum allowed length of parsed stack
     */
    private static final int MAX_PARSED_STACK_LENGTH = 32768;

    /**
     * Maximum number of allowed nested exceptions
     */
    private static final int MAX_EXCEPTION_COUNT_TO_SAVE = 10;


    private ExceptionTelemetry() {
        super();
        data = new ExceptionData();
        initialize(data.getProperties());
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
     * @deprecated
     * Gets the value indicated where the exception was handled.
     * @return The value indicating the exception
     */
    @Deprecated
    public ExceptionHandledAt getExceptionHandledAt() {
        return ExceptionHandledAt.Unhandled;
    }

    /**
     * @deprecated
     * Sets the value indicated where the exception was handled.
     * @param value The value indicating the exception
     */
    @Deprecated
    public void setExceptionHandledAt(ExceptionHandledAt value) {

    }

    /**
     * Gets a map of application-defined exception metrics. 
     * The metrics appear along with the exception in Analytics, but under Custom Metrics in Metrics Explorer.
     * @return The map of metrics
     */
    public ConcurrentMap<String,Double> getMetrics() {
        return data.getMeasurements();
    }

    public void setSeverityLevel(SeverityLevel severityLevel) {
        data.setSeverityLevel(severityLevel == null ? null : com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.values()[severityLevel.getValue()]);
    }

    public SeverityLevel getSeverityLevel() {
        return data.getSeverityLevel() == null ? null : SeverityLevel.values()[data.getSeverityLevel().getValue()];
    }

    @Override
    public Double getSamplingPercentage() {
        return samplingPercentage;
    }

    @Override
    public void setSamplingPercentage(Double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
    }

    @Deprecated
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

        //using LinkedList for efficiency in inserting at start
        List<ExceptionDetails> exceptions = new LinkedList<>();
        convertExceptionTree(throwable, null, exceptions, stackSize);

        //Trim if total exceptions exceed max permissible limit, add custom exception to indicate the same
        if (exceptions.size() > MAX_EXCEPTION_COUNT_TO_SAVE) {
            Exception e = new Exception(String.format("number of inner exception was %d which was larger than %d", exceptions.size(),
                    MAX_EXCEPTION_COUNT_TO_SAVE));

            //keep first N (MAX_PARSED_STACK_LENGTH) exceptions
            exceptions = exceptions.subList(0, MAX_EXCEPTION_COUNT_TO_SAVE);

            //we add our new exception and parent it to root exception (first in the list)
            exceptions.add(0, createWithStackInfo(e, exceptions.get(0)));
        }

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

        ExceptionDetails exceptionDetails = getExceptionDetailsWithoutParsedStack(exception, parentExceptionDetails);

        StackTraceElement[] trace = exception.getStackTrace();

        if (trace != null && trace.length > 0) {

            List<StackFrame> stack = exceptionDetails.getParsedStack();
            exceptionDetails.setHasFullStack(true);

            int stackLength = 0;
            // We need to present the stack trace in reverse order.

            for (int idx = 0; idx < trace.length; idx++) {
                StackTraceElement elem = trace[idx];

                if (elem.isNativeMethod()) {
                    continue;
                }

                StackFrame frame = getStackFrame(elem, idx);
                stackLength += getStackFrameLength(frame);

                if (stackLength > MAX_PARSED_STACK_LENGTH) {

                    //Stack is truncated
                    exceptionDetails.setHasFullStack(false);
                    break;
                }

                stack.add(frame);
            }

        }

        return exceptionDetails;
    }


    /**
     * Converts the java.lang.StackTraceElement instance into ApplicationInsights StackFrame object
     * @param element
     * @param indx
     * @return
     */
    private static StackFrame getStackFrame(StackTraceElement element, int indx) {

        StackFrame convertedFrame = new StackFrame();
        String className = element.getClassName();
        convertedFrame.setLevel(indx);
        convertedFrame.setFileName(element.getFileName());
        int lineNumber = element.getLineNumber();

        //Negative line number indicates it is not available
        if (lineNumber >= 0) {
            convertedFrame.setLine(lineNumber);
        }

        String methodName = element.getMethodName();

        if (!Strings.isNullOrEmpty(className)) {
            convertedFrame.setMethod(className + "." + methodName);
        }
        else {
            convertedFrame.setMethod(methodName);
        }
        return convertedFrame;

    }

    /**
     * returns the character length of the StackFrame, assembly is not taken into account as it is not set in java
     * @param stackFrame
     * @return
     */
    private static int getStackFrameLength(StackFrame stackFrame) {

        int len = (stackFrame.getMethod() == null ? 0 : stackFrame.getMethod().length())
                    + (stackFrame.getFileName() == null ? 0 : stackFrame.getFileName().length());
        return len;
    }


    /**
     * Creates a bare-bone ExceptionDetails object with basic information. Doesn't contain the ParsedStack of the
     * exception.
     * @param exception
     * @param parentExceptionDetails
     * @return
     */
    private static ExceptionDetails getExceptionDetailsWithoutParsedStack(Throwable exception, ExceptionDetails parentExceptionDetails) {

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
        return exceptionDetails;
    }

    @Override
    public String getEnvelopName() {
        return ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return BASE_TYPE;
    }
}
