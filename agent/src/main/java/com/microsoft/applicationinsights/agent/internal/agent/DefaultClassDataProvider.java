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
import java.util.HashMap;
import java.util.Collection;

import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

/**
 * Created by gupele on 5/11/2015.
 */
class DefaultClassDataProvider implements ClassDataProvider {
    private final static String HIBERNATE_SESSION_IMPL_CLASS_NAME = "org/hibernate/impl/SessionImpl";
    private final static String HIBERNATE_STATELESS_SESSION_IMPL_CLASS_NAME = "org/hibernate/impl/StatelessSessionImpl";

    private final static String HTTP_CLASS_NAME = "sun/net/www/protocol/http/HttpURLConnection$HttpInputStream";
    private final static String HTTP_METHOD_NAME = "read";
    private final static String HTTP_METHOD_SIGNATURE = "([BII)I";

    private final static String[] EXCLUDED_CLASS_PREFIXES = new String[] {
        "java/",
        "javax/",
        "org/apache",
        "com/microsoft/applicationinsights",
        "sun/nio/",
        "sun/rmi/",
        "com/sun/jmx/",
        "sun/net/www/http/KeepAlive",
        "com.google"
    };

    private final static String[] JDBC_CLASS_NAMES = new String[] {
        "com/mysql/jdbc/StatementImpl",
        "com/mysql/jdbc/PreparedStatement",
        "com/mysql/jdbc/ServerPreparedStatement",
        "com/mysql/jdbc/CallableStatement",
        "com/mysql/jdbc/JDBC4CallableStatement",
        "com/mysql/jdbc/JDBC4PreparedStatement",
        "com/mysql/jdbc/JDBC4ServerPreparedStatement",
        "com/mysql/jdbc/jdbc2/optional/StatementWrapper",
        "com/mysql/jdbc/jdbc2/optional/JDBC4StatementWrapper",
        "com/mysql/jdbc/jdbc2/optional/CallableStatementWrapper",
        "com/mysql/jdbc/jdbc2/optional/JDBC4PreparedStatementWrapper",

        "org/sqlite/jdbc4/JDBC4Statement",
        "org/sqlite/core/CorePreparedStatement",
        "org/sqlite/jdbc3/JDBC3PreparedStatement",
        "org/sqlite/jdbc4/JDBC4PreparedStatement",

        "org/hsqldb/jdbc/JDBCPreparedStatement",
        "org/hsqldb/jdbc/jdbcCallableStatement",
        "org/hsqldb/jdbc/JDBCStatement",

        "org/postgresql/core/BaseStatement",
        "org/postgresql/jdbc2/AbstractJdbc2Statement",
        "org/postgresql/jdbc3g/AbstractJdbc3gStatement",
        "org/postgresql/jdbc4/AbstractJdbc4Statement",
        "org/postgresql/jdbc4/Jdbc4Statement",
        "org/postgresql/jdbc4/Jdbc4PreparedStatement",
        "org/postgresql/jdbc4/Jdbc4CallableStatement"
    };

    private final static String[] JDBC_METHODS_TO_TRACK = {
        "execute",
        "executeQuery",
        "executeUpdate"
    };

    private final static String[] HIBERNATE_SESSION_IMPL_METHODS_TO_TRACK = {
        "delete",
        "execute",
        "executeNativeUpdate",
        "executeUpdate",
        "find",
        "get",
        "save",
        "list",
        "load",
        "saveOrUpdate",
        "update"
    };

    private final static String[] HIBERNATE_STATELESS_SESSION_IMPL_METHODS_TO_TRACK = {
        "delete",
        "get",
        "insert",
        "list",
        "update"
    };

    private AgentConfiguration agentConfiguration;

    private final HashSet<String> sqlClasses = new HashSet<String>();
    private final HashSet<String> httpClasses = new HashSet<String>();
    private final HashSet<String> excludedPaths;

    private final HashMap<String, ClassInstrumentationData> classesToInstrument = new HashMap<String, ClassInstrumentationData>();

    private boolean builtInEnabled = true;

    public DefaultClassDataProvider() {
        excludedPaths = new HashSet<String>((Arrays.asList(EXCLUDED_CLASS_PREFIXES)));
    }

    @Override
    public void setConfiguration(AgentConfiguration agentConfiguration) {
        this.agentConfiguration = agentConfiguration;
        addConfigurationData();

        if (builtInEnabled) {
            InternalAgentLogger.INSTANCE.trace("Adding built-in instrumentation");

            populateSqlClasses();
            populateHttpClasses();
            addHibernate();
        }
    }

    @Override
    public boolean isSqlClass(String className) {
        return sqlClasses.contains(className);
    }

    @Override
    public boolean isHttpClass(String className) {
        return httpClasses.contains(className);
    }

    /**
     * Gets the {@link ClassInstrumentationData} that is associated
     * with the argument 'className', and removes that entry from the container once this is found
     * @param className The class name to search for
     * @return The {@link ClassInstrumentationData}
     */
    @Override
    public ClassInstrumentationData getAndRemove(String className) {
        return classesToInstrument.remove(className);
    }

    private void addHibernate() {
        HashSet<String> methodNames = new HashSet<String>(Arrays.asList(HIBERNATE_SESSION_IMPL_METHODS_TO_TRACK));

        addToClasses(HIBERNATE_SESSION_IMPL_CLASS_NAME, InstrumentedClassType.SQL, methodNames);

        methodNames.clear();
        methodNames.addAll(Arrays.asList(HIBERNATE_STATELESS_SESSION_IMPL_METHODS_TO_TRACK));

        addToClasses(HIBERNATE_STATELESS_SESSION_IMPL_CLASS_NAME,InstrumentedClassType.SQL, methodNames);
    }

    private void populateSqlClasses() {
        sqlClasses.addAll(Arrays.asList(JDBC_CLASS_NAMES));

        HashSet<String> methodNamesOnly = new HashSet<String>(Arrays.asList(JDBC_METHODS_TO_TRACK));

        for (String className : sqlClasses) {
            addToClasses(className, InstrumentedClassType.SQL, methodNamesOnly);
        }
    }

    private void populateHttpClasses() {
        httpClasses.add(HTTP_CLASS_NAME);

        ClassInstrumentationData data =
                new ClassInstrumentationData(HTTP_CLASS_NAME, InstrumentedClassType.HTTP)
                    .setReportCaughtExceptions(false)
                    .setReportExecutionTime(true);
        data.addMethod(HTTP_METHOD_NAME, HTTP_METHOD_SIGNATURE, false, true);

        classesToInstrument.put(data.getClassName(), data);
    }

    private boolean isExcluded(String className) {
        for (String f : excludedPaths) {
            if (className.startsWith(f)) {
                return true;
            }
        }
        return false;
    }

    private void addConfigurationData() {
        if (agentConfiguration == null) {
            return;
        }

        builtInEnabled = agentConfiguration.getBuiltInConfiguration().isEnabled();

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

    private void addToClasses(String className, InstrumentedClassType type, Collection<String> methodNamesOnly) {
        ClassInstrumentationData data =
                new ClassInstrumentationData(className, type)
                .setReportCaughtExceptions(false)
                .setReportExecutionTime(true);
        for (String methodName : methodNamesOnly) {
            data.addMethod(methodName, null, false, true);
        }
        classesToInstrument.put(className, data);
    }
}
