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

import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.applicationinsights.agent.internal.agent.exceptions.RuntimeExceptionProvider;
import com.microsoft.applicationinsights.agent.internal.agent.http.HttpClassDataProvider;
import com.microsoft.applicationinsights.agent.internal.agent.redis.JedisClassDataProvider;
import com.microsoft.applicationinsights.agent.internal.agent.sql.PreparedStatementClassDataProvider;
import com.microsoft.applicationinsights.agent.internal.agent.sql.StatementClassDataDataProvider;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

/**
 * Created by gupele on 5/11/2015.
 */
class DefaultClassDataProvider implements ClassDataProvider {

    private final static String[] EXCLUDED_CLASS_PREFIXES = new String[] {
        "java/",
        "javax/",
        "org/apache",
        "com/microsoft/applicationinsights",
        "com/mysql/",
        "org/sqlite/",
        "org/hsqldb/",
        "org/postgresql/",
        "org/postgresql/",
        "sun/nio/",
        "sun/rmi/",
        "com/sun/jmx/",
        "sun/net/www/http/KeepAlive",
        "com.google"
    };

    private final HashSet<String> excludedPaths;

    private final ConcurrentHashMap<String, ClassInstrumentationData> classesToInstrument = new ConcurrentHashMap<String, ClassInstrumentationData>();

    private boolean builtInEnabled = true;

    public DefaultClassDataProvider() {
        excludedPaths = new HashSet<String>((Arrays.asList(EXCLUDED_CLASS_PREFIXES)));
    }

    @Override
    public void setConfiguration(AgentConfiguration agentConfiguration) {
        setBuiltInDataFlag(agentConfiguration);

        if (builtInEnabled) {
            InternalAgentLogger.INSTANCE.trace("Adding built-in instrumentation");

            if (agentConfiguration.getBuiltInConfiguration().isJdbcEnabled()) {
                new StatementClassDataDataProvider(classesToInstrument).add();
                new PreparedStatementClassDataProvider(classesToInstrument).add();
            }

            if (agentConfiguration.getBuiltInConfiguration().isHttpEnabled()) {
                new HttpClassDataProvider(classesToInstrument).add();
            }

            if (agentConfiguration.getBuiltInConfiguration().isRedisEnabled()) {
                new JedisClassDataProvider(classesToInstrument).add();
            }

            if (agentConfiguration.getBuiltInConfiguration().getDataOfConfigurationForException().isEnabled()) {
                new RuntimeExceptionProvider(classesToInstrument).add();
            }
        }

        addConfigurationData(agentConfiguration);
    }

    /**
     * Gets the {@link ClassInstrumentationData} that is associated
     * with the argument 'className', and removes that entry from the container once this is found
     * @param className The class name to search for
     * @return The {@link ClassInstrumentationData}
     */
    @Override
    public DefaultByteCodeTransformer getAndRemove(String className) {
        final ClassInstrumentationData classInstrumentationData = classesToInstrument.remove(className);
        if (classInstrumentationData == null) {
            return null;
        }

        return new DefaultByteCodeTransformer(classInstrumentationData);
    }

    private boolean isExcluded(String className) {
        for (String f : excludedPaths) {
            if (className.startsWith(f)) {
                return true;
            }
        }
        return false;
    }

    private void addConfigurationData(AgentConfiguration agentConfiguration) {
        if (agentConfiguration == null) {
            return;
        }

        Map<String, ClassInstrumentationData> configurationData = agentConfiguration.getRequestedClassesToInstrument();
        if (configurationData != null) {
            for (ClassInstrumentationData classInstrumentationData : configurationData.values()) {
                if (isExcluded(classInstrumentationData.getClassName())) {
                    InternalAgentLogger.INSTANCE.trace("'%s' is not added since it is not allowed", classInstrumentationData.getClassName());
                    continue;
                }

                InternalAgentLogger.INSTANCE.trace("Adding '%s'", classInstrumentationData.getClassName());
                classesToInstrument.put(classInstrumentationData.getClassName(), classInstrumentationData);
            }
        }

        excludedPaths.addAll(agentConfiguration.getExcludedPrefixes());
    }

    private void setBuiltInDataFlag(AgentConfiguration agentConfiguration) {
        if (agentConfiguration == null) {
            return;
        }
        builtInEnabled = agentConfiguration.getBuiltInConfiguration().isEnabled();
    }
}
