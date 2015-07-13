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
 * The class holds the type of actions that should be done on an instrumented method
 * The class is the 'decision' for the method after taking into consideration init/configuration data
 *
 * Note, it is recommended to build an instance with {@link com.microsoft.applicationinsights.agent.internal.agent.MethodInstrumentationDecisionBuilder}
 *
 * Created by gupele on 5/31/2015.
 */
final class MethodInstrumentationDecision {
    private final boolean reportCaughtExceptions;
    private final boolean reportExecutionTime;

    public MethodInstrumentationDecision(boolean reportCaughtExceptions, boolean reportExecutionTime) {
        this.reportCaughtExceptions = reportCaughtExceptions;
        this.reportExecutionTime = reportExecutionTime;
    }

    public boolean isReportCaughtExceptions() {
        return reportCaughtExceptions;
    }

    public boolean isReportExecutionTime() {
        return reportExecutionTime;
    }
}
