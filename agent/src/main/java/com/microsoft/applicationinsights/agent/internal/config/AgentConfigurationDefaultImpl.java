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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;

/**
 * Created by gupele on 5/19/2015.
 */
final class AgentConfigurationDefaultImpl implements AgentConfiguration {
    private HashMap<String, ClassInstrumentationData> classesToInstrument;
    private AgentBuiltInConfiguration builtInConfiguration = new AgentBuiltInConfigurationBuilder().create();
    private Set<String> forbiddenPrefixes = new HashSet<String>();

    void setRequestedClassesToInstrument(HashMap<String, ClassInstrumentationData> classesToInstrument) {
        this.classesToInstrument = classesToInstrument;
    }

    @Override
    public Map<String, ClassInstrumentationData> getRequestedClassesToInstrument() {
        return classesToInstrument;
    }

    @Override
    public AgentBuiltInConfiguration getBuiltInSwitches() {
        return builtInConfiguration;
    }

    @Override
    public Set<String> getForbiddenPrefixes() {
        return forbiddenPrefixes;
    }

    public void setBuiltInData(AgentBuiltInConfiguration builtInData) {
        this.builtInConfiguration = builtInData;
    }

    public void setForbiddenPrefixes(Set<String> forbiddenPrefixes) {
        this.forbiddenPrefixes = forbiddenPrefixes;
    }
}
