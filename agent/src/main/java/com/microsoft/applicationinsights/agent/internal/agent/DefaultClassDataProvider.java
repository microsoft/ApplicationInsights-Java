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

import com.microsoft.applicationinsights.agent.internal.agent.exceptions.RuntimeExceptionProvider;
import com.microsoft.applicationinsights.agent.internal.agent.http.HttpClassDataProvider;
import com.microsoft.applicationinsights.agent.internal.agent.redis.JedisClassDataProvider;
import com.microsoft.applicationinsights.agent.internal.agent.sql.PreparedStatementClassDataProvider;
import com.microsoft.applicationinsights.agent.internal.agent.sql.StatementClassDataDataProvider;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

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
        "sun/nio/",
        "sun/rmi/",
        "com/sun/jmx/",
        "sun/net/www/http/KeepAlive",
        "com.google"
    };

    private final HashSet<String> excludedPaths;

    private final ConcurrentHashMap<String, ClassInstrumentationData> classesToInstrument = new ConcurrentHashMap<String, ClassInstrumentationData>();
    private final ConcurrentHashMap<String, ClassInstrumentationData> regExpClassesToInstrument = new ConcurrentHashMap<String, ClassInstrumentationData>();

    private boolean builtInEnabled = true;

    private boolean debugMode = false;

    public DefaultClassDataProvider() {
        excludedPaths = new HashSet<String>((Arrays.asList(EXCLUDED_CLASS_PREFIXES)));
    }

    @Override
    public void setConfiguration(AgentConfiguration agentConfiguration) {
        debugMode = agentConfiguration.isDebugMode();

        setBuiltInDataFlag(agentConfiguration);

        if (builtInEnabled) {
            InternalLogger.INSTANCE.trace("Adding built-in instrumentation");

            if (agentConfiguration.getBuiltInConfiguration().isJdbcEnabled()) {
				InternalLogger.INSTANCE.trace("Adding built-in JDBC Statements instrumentation");
                new StatementClassDataDataProvider(classesToInstrument).add();

				InternalLogger.INSTANCE.trace("Adding built-in JDBC Prepared Statements instrumentation");
                new PreparedStatementClassDataProvider(classesToInstrument).add();
            }

            if (agentConfiguration.getBuiltInConfiguration().isHttpEnabled()) {
				InternalLogger.INSTANCE.trace("Adding built-in HTTP instrumentation");
                HttpClassDataProvider httpClassDataProvider= new HttpClassDataProvider(classesToInstrument);
                httpClassDataProvider.setIsW3CEnabled(agentConfiguration.getBuiltInConfiguration().isW3cEnabled());
                httpClassDataProvider.setIsW3CBackportEnabled(agentConfiguration.getBuiltInConfiguration().isW3CBackportEnabled());
                httpClassDataProvider.add();
            }

            if (agentConfiguration.getBuiltInConfiguration().isRedisEnabled()) {
				InternalLogger.INSTANCE.trace("Adding built-in Jedis instrumentation");
                new JedisClassDataProvider(classesToInstrument).add();
            }

            if (agentConfiguration.getBuiltInConfiguration().getDataOfConfigurationForException().isEnabled()) {
				InternalLogger.INSTANCE.trace("Adding built-in Runtime instrumentation");
                new RuntimeExceptionProvider(classesToInstrument).add();
            }

            addConfigurationData(agentConfiguration.getBuiltInConfiguration().getSimpleBuiltInClasses());
        }

        Collection<ClassInstrumentationData> requestedClsssesToInstrument = agentConfiguration.getRequestedClassesToInstrument().values();
        addConfigurationData(requestedClsssesToInstrument);

        excludedPaths.addAll(agentConfiguration.getExcludedPrefixes());
    }

    /**
     * Gets the {@link ClassInstrumentationData} that is associated
     * with the argument 'className', and removes that entry from the container once this is found
     * If not found, the method will try to find a match using a regular expression in the regex container
     * @param className The class name to search for
     * @return The {@link ClassInstrumentationData}
     */
    @Override
    public DefaultByteCodeTransformer getAndRemove(String className) {
        ClassInstrumentationData classInstrumentationData = classesToInstrument.remove(className);
        if (classInstrumentationData == null) {
            if (!regExpClassesToInstrument.isEmpty()) {
                int index = className.lastIndexOf('/');
                if (index != -1) {
                    String fullPackageName = className.substring(0, index + 1);
                    String onlycClassName = className.substring(index + 1);
                    classInstrumentationData = regExpClassesToInstrument.get(fullPackageName);
                    if (classInstrumentationData == null) {
                        return null;
                    }
					if (!classInstrumentationData.isClassNameMatches(onlycClassName)) {
						return null;
					}

					ClassInstrumentationData newClassInstrumentationData = new ClassInstrumentationData(className, classInstrumentationData.getClassType(), classInstrumentationData.getClassVisitorFactory());
					newClassInstrumentationData.setMethodInstrumentationInfo(classInstrumentationData.getMethodInstrumentationInfo());
					classInstrumentationData = newClassInstrumentationData;
					InternalLogger.INSTANCE.trace("Adding %s", classInstrumentationData.getFullPackageName());
               }
            }
        }

        if (classInstrumentationData == null) {
            return null;
        }

        ImplementationsCoordinator.INSTANCE.addClassNameToType(classInstrumentationData.getClassName(), classInstrumentationData.getClassType());
        DefaultByteCodeTransformer transformer = new DefaultByteCodeTransformer(classInstrumentationData, debugMode);

        return transformer;
    }

    private boolean isExcluded(String className) {
        for (String f : excludedPaths) {
            if (className.startsWith(f)) {
                return true;
            }
        }
        return false;
    }

    private void addConfigurationData(Collection<ClassInstrumentationData> requestedClassesToInstrument) {
        if (requestedClassesToInstrument == null) {
            return;
        }

        for (ClassInstrumentationData classInstrumentationData : requestedClassesToInstrument) {
            if (!classInstrumentationData.isRegExp()) {
                if (isExcluded(classInstrumentationData.getClassName())) {
                    InternalLogger.INSTANCE.trace("'%s' is not added since it is not allowed", classInstrumentationData.getClassName());
                    continue;
                }
                InternalLogger.INSTANCE.trace("Adding '%s'", classInstrumentationData.getClassName());
            } else {
                InternalLogger.INSTANCE.trace("Adding regex classes in package'%s'", classInstrumentationData.getFullPackageName());
            }

            if (classInstrumentationData.isRegExp()) {
                regExpClassesToInstrument.put(classInstrumentationData.getFullPackageName(), classInstrumentationData);
            } else {
                classesToInstrument.put(classInstrumentationData.getClassName(), classInstrumentationData);
            }
        }
    }

    private void setBuiltInDataFlag(AgentConfiguration agentConfiguration) {
        if (agentConfiguration == null) {
            return;
        }
        builtInEnabled = agentConfiguration.getBuiltInConfiguration().isEnabled();
    }
}
