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

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;

public class ClassInstrumentationData {

    public static final String OTHER_TYPE = "OTHER";

    public static final String ANY_SIGNATURE_MARKER = "";

    private final String classType;

    private final long thresholdInMS;

    // first key is method name, second key is signature or ANY_SIGNATURE_MARKER
    private final Map<String, Map<String, MethodInfo>> methodInfos = new HashMap<>();

    public ClassInstrumentationData(String classType, long thresholdInMS) {
        this.classType = classType;
        this.thresholdInMS = thresholdInMS;
    }

    public String getClassType() {
        return classType;
    }

    public long getThresholdInMS() {
        return thresholdInMS;
    }

    public Map<String, Map<String, MethodInfo>> getMethodInfos() {
        return methodInfos;
    }

    public void addMethod(String methodName, String signature, long thresholdInMS) {

        Map<String, MethodInfo> innerMap = methodInfos.get(methodName);
        if (innerMap == null) {
            innerMap = new HashMap<>();
            methodInfos.put(methodName, innerMap);
        }

        MethodInfo methodInfo = new MethodInfo(thresholdInMS);

        if (Strings.isNullOrEmpty(signature)) {
            innerMap.put(ANY_SIGNATURE_MARKER, methodInfo);
        } else {
            innerMap.put(signature, methodInfo);
        }
    }
}
