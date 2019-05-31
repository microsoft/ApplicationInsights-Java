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
package com.microsoft.applicationinsights.agent.internal.config;

import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;

/**
 * An 'instrumented' class data
 */
public class ClassInstrumentationData {

    public static final String OTHER_TYPE = "OTHER";

    public static final String ANY_SIGNATURE_MARKER = "";

    private final String className;

    // The type of class
    private final String classType;

    // first key is method name, second key is signature or ANY_SIGNATURE_MARKER
    private final Map<String, Map<String, MethodInfo>> methodInfos = new HashMap<>();

    private MethodInfo allClassMethods = null;

    private final long thresholdInMS;
    private final boolean reportExecutionTime;
    private final boolean reportCaughtExceptions;

    public ClassInstrumentationData(String className, String classType, boolean reportCaughtExceptions,
                                    boolean reportExecutionTime, long thresholdInMS) {
        this.classType = classType;
        this.className = className;

        this.reportCaughtExceptions = reportCaughtExceptions;
        this.reportExecutionTime = reportExecutionTime;
        this.thresholdInMS = thresholdInMS;
    }

    public Map<String, Map<String, MethodInfo>> getMethodInfos() {
        return methodInfos;
    }

    public MethodInfo getAllClassMethods() {
        return allClassMethods;
    }

    public void addMethod(String methodName, String signature, boolean reportCaughtExceptions,
                          boolean reportExecutionTime, long thresholdInMS) {

        if (Strings.isNullOrEmpty(methodName)) {
            return;
        }

        if (allClassMethods != null) {
            throw new IllegalStateException();
        }

        if (!reportCaughtExceptions && !reportExecutionTime) {
            return;
        }

        Map<String, MethodInfo> innerMap = methodInfos.get(methodName);
        if (innerMap == null) {
            innerMap = new HashMap<>();
            methodInfos.put(methodName, innerMap);
        }

        MethodInfo methodInfo = new MethodInfo(reportCaughtExceptions, reportExecutionTime, thresholdInMS);

        if (Strings.isNullOrEmpty(signature)) {
            innerMap.put(ANY_SIGNATURE_MARKER, methodInfo);
        } else {
            innerMap.put(signature, methodInfo);
        }
    }

    public void addAllMethods(boolean reportCaughtExceptions, boolean reportExecutionTime) {
        if (!methodInfos.isEmpty()) {
            throw new IllegalStateException();
        }

        if (!reportCaughtExceptions && !reportExecutionTime) {
            return;
        }

        if (allClassMethods == null) {
            allClassMethods = new MethodInfo(reportCaughtExceptions, reportExecutionTime, 0);
        }
    }

    public boolean isEmpty() {
        return methodInfos.isEmpty() && allClassMethods == null;
    }

    public String getClassName() {
        return className;
    }

    public String getClassType() {
        return classType;
    }

    public boolean isReportExecutionTime() {
        return reportExecutionTime;
    }

    public boolean isReportCaughtExceptions() {
        return reportCaughtExceptions;
    }

    public long getThresholdInMS() {
        return thresholdInMS;
    }
}
