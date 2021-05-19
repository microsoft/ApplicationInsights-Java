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
package com.microsoft.applicationinsights.agent;

import java.lang.instrument.Instrumentation;

import io.opentelemetry.javaagent.OpenTelemetryAgent;

// IMPORTANT!! If this class is renamed, be sure to add the previous name to DuplicateAgentClassFileTransformer
// so that previous versions will be suppressed (current versions with the same class name are suppressed
// below via the alreadyLoaded flag
public class Agent {

    // this is to prevent the agent from loading and instrumenting everything twice
    // (leading to unpredictable results) when -javaagent:applicationinsights-agent.jar
    // appears multiple times on the command line
    private static volatile boolean alreadyLoaded;

    public static void premain(String agentArgs, Instrumentation inst) {
        if (alreadyLoaded) {
            return;
        }
        OpenTelemetryAgent.premain(agentArgs, inst, Agent.class);
        alreadyLoaded = true;
    }

    // this is provided only for dynamic attach in the first line of main
    // there are many problematic edge cases around dynamic attach any later than that
    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
