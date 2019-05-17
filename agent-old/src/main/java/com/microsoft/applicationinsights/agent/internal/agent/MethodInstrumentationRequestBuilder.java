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

/**
 * Created by gupele on 6/2/2015.
 */
final class MethodInstrumentationRequestBuilder {
    private String methodName;
    public String methodSignature;
    public boolean reportCaughtExceptions;
    public boolean reportExecutionTime;
    public long thresholdInMS;

    public MethodInstrumentationRequestBuilder withMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public MethodInstrumentationRequestBuilder withMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
        return this;
    }

    public MethodInstrumentationRequestBuilder withReportCaughtExceptions(boolean reportCaughtExceptions) {
        this.reportCaughtExceptions = reportCaughtExceptions;
        return this;
    }

    public MethodInstrumentationRequestBuilder withReportExecutionTime(boolean reportExecutionTime) {
        this.reportExecutionTime = reportExecutionTime;
        return this;
    }

    public MethodInstrumentationRequestBuilder withThresholdInMS(long thresholdInMS) {
        this.thresholdInMS = thresholdInMS;
        return this;
    }

    public MethodInstrumentationRequest create() {
        return new MethodInstrumentationRequest(methodName, methodSignature, reportCaughtExceptions, reportExecutionTime, thresholdInMS);
    }
}
