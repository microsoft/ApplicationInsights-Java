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

package com.microsoft.applicationinsights.agent.internal;

import java.io.File;
import java.util.List;

import com.microsoft.applicationinsights.agent.internal.config.BuiltInInstrumentation;
import com.microsoft.applicationinsights.agent.internal.config.XmlAgentConfigurationBuilder;
import com.microsoft.applicationinsights.internal.config.AgentXmlElement;
import com.microsoft.applicationinsights.internal.config.AgentXmlElement.InstrumentationXmlElement;
import com.microsoft.applicationinsights.internal.config.AgentXmlElement.W3CXmlElement;
import com.microsoft.applicationinsights.internal.config.ParamXmlElement;
import org.checkerframework.checker.nullness.qual.Nullable;

class AIAgentXmlLoader {

    static @Nullable AgentXmlElement load(File agentJarParentFile) {
        BuiltInInstrumentation builtInInstrumentation =
                new XmlAgentConfigurationBuilder().parseConfigurationFile(agentJarParentFile.getAbsolutePath());
        if (!builtInInstrumentation.isEnabled()) {
            return null;
        }
        AgentXmlElement agentXmlElement = new AgentXmlElement();
        List<InstrumentationXmlElement> instrumentationXmlElement = agentXmlElement.getInstrumentation();
        if (builtInInstrumentation.isHttpEnabled()) {
            W3CXmlElement w3cConfiguration = new W3CXmlElement();
            w3cConfiguration.setEnabled(builtInInstrumentation.isW3cEnabled());
            w3cConfiguration.setBackCompatEnabled(builtInInstrumentation.isW3cBackCompatEnabled());
        } else {
            instrumentationXmlElement.add(newInstrumentationXmlElement("apache-http-client", false));
            instrumentationXmlElement.add(newInstrumentationXmlElement("okhttp", false));
        }
        if (builtInInstrumentation.isJdbcEnabled()) {
            InstrumentationXmlElement jdbcConfiguration = newInstrumentationXmlElement("jdbc", true);
            jdbcConfiguration.getParams().add(newParam("explainPlanThresholdInMS",
                    Long.toString(builtInInstrumentation.getQueryPlanThresholdInMS())));
            instrumentationXmlElement.add(jdbcConfiguration);
        } else {
            instrumentationXmlElement.add(newInstrumentationXmlElement("jdbc", false));
        }
        if (!builtInInstrumentation.isLoggingEnabled()) {
            instrumentationXmlElement.add(newInstrumentationXmlElement("log4j", false));
            instrumentationXmlElement.add(newInstrumentationXmlElement("logback", false));
        }
        if (!builtInInstrumentation.isJedisEnabled()) {
            instrumentationXmlElement.add(newInstrumentationXmlElement("redis", false));
        }
        return agentXmlElement;
    }

    private static InstrumentationXmlElement newInstrumentationXmlElement(String name, boolean enabled) {
        InstrumentationXmlElement instrumentationXmlElement = new InstrumentationXmlElement();
        instrumentationXmlElement.setName(name);
        instrumentationXmlElement.setEnabled(enabled);
        return instrumentationXmlElement;
    }

    private static ParamXmlElement newParam(String name, String value) {
        ParamXmlElement param = new ParamXmlElement();
        param.setName(name);
        param.setValue(value);
        return param;
    }
}
