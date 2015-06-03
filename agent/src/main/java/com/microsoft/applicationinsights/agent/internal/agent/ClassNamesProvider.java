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

import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;

/**
 * Defines the interface for classes that know to supply
 * The needed classes that can be instrumented by the agent
 *
 * Created by gupele on 5/11/2015.
 */
interface ClassNamesProvider {
    /**
     * The configuration that might add extra information
     * @param agentConfiguration The configuration
     */
    void setConfiguration(AgentConfiguration agentConfiguration);

    /**
     * Will return true if the class name is considered as 'Sql' class
     * @param className The class name to check
     * @return True if that is an 'Sql' class, false otherwise
     */
    boolean isSqlClass(String className);

    /**
     * Will return true if the class name is considered as 'Http' class
     * @param className The class name to check
     * @return True if that is an 'Http' class, false otherwise
     */
    boolean isHttpClass(String className);

    /**
     * Get the {@link ClassInstrumentationData}
     * that is associated with the class name, if such information is found it is removed from the container
     * @param className The class name to search
     * @return {@link ClassInstrumentationData} that is
     * associated with the class name, null otherwise
     */
    ClassInstrumentationData getAndRemove(String className);
}
