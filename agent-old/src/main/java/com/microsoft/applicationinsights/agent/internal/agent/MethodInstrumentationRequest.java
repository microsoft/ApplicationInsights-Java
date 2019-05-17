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

package com.microsoft.applicationinsights.agent.internal.agent;

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;

/**
 * Represents a request for instrumenting a method.
 * methodName: Must be a non-null non empty value
 * methodSignature: If null then all methods with 'methodName' are qualified
 * reportCaughtExceptions: If true will report caught exceptions within the method
 * reportExecutionTime: If true will report the time to execute the method
 *
 * Note, it is recommended to build an instance with {@link com.microsoft.applicationinsights.agent.internal.agent.MethodInstrumentationRequestBuilder}
 *
 * Created by gupele on 5/31/2015.
 */
final class MethodInstrumentationRequest {
    private final String methodName;
    private final String methodSignature;
    private final boolean reportCaughtExceptions;
    private final boolean reportExecutionTime;
    private final long thresholdInMS;

    public MethodInstrumentationRequest(String methodName, String methodSignature, boolean reportCaughtExceptions, boolean reportExecutionTime, long thresholdInMS) {
        if (StringUtils.isNullOrEmpty(methodName)) {
            throw new IllegalArgumentException("methodName must be non-null non-empty value.");
        }

        this.reportCaughtExceptions = reportCaughtExceptions;
        this.reportExecutionTime = reportExecutionTime;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.thresholdInMS = thresholdInMS;
    }

    public MethodInstrumentationRequest(String methodName, boolean reportCaughtExceptions, boolean reportExecutionTime) {
        this(methodName, null, reportCaughtExceptions, reportExecutionTime, 0);
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public boolean isReportCaughtExceptions() {
        return reportCaughtExceptions;
    }

    public boolean isReportExecutionTime() {
        return reportExecutionTime;
    }

    public long getThresholdInMS() {
        return thresholdInMS;
    }
}
