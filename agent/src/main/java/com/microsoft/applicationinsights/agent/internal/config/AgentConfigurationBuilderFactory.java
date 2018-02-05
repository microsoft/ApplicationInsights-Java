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

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

/**
 * The factory is responsible for creating the correct {@link com.microsoft.applicationinsights.agent.internal.config.AgentConfigurationBuilder}
 *
 * Created by gupele on 5/19/2015.
 */
public class AgentConfigurationBuilderFactory {
    public AgentConfigurationBuilder createBuilder(String builderClassName) {
        if (StringUtils.isNullOrEmpty(builderClassName)) {
            return createDefaultBuilder();
        }
        try {
            Object builder = Class.forName(builderClassName).newInstance();
            if (builder instanceof AgentConfigurationBuilder) {
                return (AgentConfigurationBuilder)builder;
            }
        } catch (Throwable t) {
            InternalAgentLogger.INSTANCE.error("Failed to create builder: '%s'", t.toString());
        }

        return null;
    }

    public AgentConfigurationBuilder createDefaultBuilder() {
        return new XmlAgentConfigurationBuilder();
    }
}
