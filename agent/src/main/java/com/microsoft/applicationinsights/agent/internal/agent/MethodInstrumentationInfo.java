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

import java.util.HashMap;

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;

/**
 * The class holds the data for methods.
 * The class should collect data for methods of one class.
 *
 * For each method the class holds a {@link com.microsoft.applicationinsights.agent.internal.agent.MethodInstrumentationDecision}
 * that will be used later on if the method of the class is found in runtime.
 *
 * The class knows how to 'merge' the information of methods so methods without signatures
 * mean that all methods with that name qualify. to mark all the methods, set priority between methods
 *
 * Created by gupele on 5/29/2015.
 */
public final class MethodInstrumentationInfo {
    private final static String ANY_SIGNATURE_MARKER = "";
    private MethodInstrumentationDecision allClassMethods = null;

    private static class MethodInfo {

        public HashMap<String, MethodInstrumentationDecision> methodsInstrumentationData = new HashMap<String, MethodInstrumentationDecision>();

        public MethodInfo(MethodInstrumentationRequest methodInstrumentationRequest, MethodVisitorFactory methodVisitorFactory) {
            if (StringUtils.isNullOrEmpty(methodInstrumentationRequest.getMethodSignature())) {
                MethodInstrumentationDecision decision =
                        new MethodInstrumentationDecisionBuilder()
                                .withReportCaughtExceptions(methodInstrumentationRequest.isReportCaughtExceptions())
                                .withReportExecutionTime(methodInstrumentationRequest.isReportExecutionTime())
                                .withMethodVisitorFactory(methodVisitorFactory)
                                .create();
                methodsInstrumentationData.put(ANY_SIGNATURE_MARKER, decision);
            }
        }

        public void add(String requestedMethodSignature, boolean reportCaughtExceptions, boolean reportExecutionTime, long thresholdInMS, MethodVisitorFactory methodVisitorFactory) {
            String methodSignature = requestedMethodSignature;
            if (StringUtils.isNullOrEmpty(methodSignature)) {
                methodSignature = ANY_SIGNATURE_MARKER;
            }

            MethodInstrumentationDecision decision =
                    new MethodInstrumentationDecisionBuilder()
                            .withReportCaughtExceptions(reportCaughtExceptions)
                            .withReportExecutionTime(reportExecutionTime)
                            .withThresholdInMS(thresholdInMS)
                            .withMethodVisitorFactory(methodVisitorFactory)
                            .create();
            methodsInstrumentationData.put(methodSignature, decision);
        }
    }

    private HashMap<String, MethodInfo> methods = new HashMap<String, MethodInfo>();

    public void addMethod(MethodInstrumentationRequest methodInstrumentationRequest) {
        addMethod(methodInstrumentationRequest, null);
    }

    public void addMethod(MethodInstrumentationRequest methodInstrumentationRequest, MethodVisitorFactory methodVisitorFactory) {
        if (allClassMethods != null) {
            throw new IllegalStateException();
        }

        if (!methodInstrumentationRequest.isReportCaughtExceptions() && !methodInstrumentationRequest.isReportExecutionTime()) {
            return;
        }

        MethodInfo info = methods.get(methodInstrumentationRequest.getMethodName());
        if (info == null) {
            info = new MethodInfo(methodInstrumentationRequest, methodVisitorFactory);
            methods.put(methodInstrumentationRequest.getMethodName(), info);
        }

        info.add(methodInstrumentationRequest.getMethodSignature(),
                methodInstrumentationRequest.isReportCaughtExceptions(),
                methodInstrumentationRequest.isReportExecutionTime(),
                methodInstrumentationRequest.getThresholdInMS(),
                methodVisitorFactory);
    }

    public MethodInstrumentationDecision getDecision(String methodName, String methodSignature) {
        if (StringUtils.isNullOrEmpty(methodName)) {
            return null;
        }

        if (allClassMethods != null) {
            return allClassMethods;
        }

        MethodInfo info = methods.get(methodName);
        if (info == null) {
            return null;
        }

        if (info.methodsInstrumentationData.containsKey(methodSignature)) {
            return info.methodsInstrumentationData.get(methodSignature);
        }

        return info.methodsInstrumentationData.get(ANY_SIGNATURE_MARKER);
    }

    public void addAllMethods(boolean reportCaughtExceptions, boolean reportExecutionTime, MethodVisitorFactory methodVisitorFactory) {
        if (!methods.isEmpty()) {
            throw new IllegalStateException();
        }

        if (!reportCaughtExceptions && !reportExecutionTime) {
            return;
        }

        if (allClassMethods == null) {
            MethodInstrumentationDecision decision =
                    new MethodInstrumentationDecisionBuilder()
                            .withReportCaughtExceptions(reportCaughtExceptions)
                            .withReportExecutionTime(reportExecutionTime)
                            .withMethodVisitorFactory(methodVisitorFactory)
                            .create();
            allClassMethods = decision;
        }
    }

    public boolean isEmpty() {
        return methods.isEmpty() && allClassMethods == null;
    }
}
