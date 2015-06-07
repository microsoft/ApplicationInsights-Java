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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

/**
 * Created by gupele on 5/11/2015.
 */
class DefaultClassNamesProvider implements ClassNamesProvider {
    private AgentConfiguration agentConfiguration;

    private final HashSet<String> sqlClasses = new HashSet<String>();
    private final HashSet<String> httpClasses = new HashSet<String>();
    private final HashSet<String> forbiddenPaths = new HashSet<String>();

    private final HashMap<String, ClassInstrumentationData> classesToInstrument = new HashMap<String, ClassInstrumentationData>();

    private boolean builtInEnabled = true;

    public DefaultClassNamesProvider() {
        populateForbiddenClasses();
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
        HashSet<String> methodNamesOnly = new HashSet<String>();
        methodNamesOnly.add("delete");
        methodNamesOnly.add("execute");
        methodNamesOnly.add("executeNativeUpdate");
        methodNamesOnly.add("executeUpdate");
        methodNamesOnly.add("find");
        methodNamesOnly.add("get");
        methodNamesOnly.add("save");
        methodNamesOnly.add("list");
        methodNamesOnly.add("load");
        methodNamesOnly.add("saveOrUpdate");
        methodNamesOnly.add("update");

        addToClasses("org/hibernate/impl/SessionImpl", InstrumentedClassType.SQL, methodNamesOnly);

        methodNamesOnly.clear();
        methodNamesOnly.add("delete");
        methodNamesOnly.add("get");
        methodNamesOnly.add("insert");
        methodNamesOnly.add("list");
        methodNamesOnly.add("update");

        addToClasses("org/hibernate/impl/StatelessSessionImpl", InstrumentedClassType.SQL, methodNamesOnly);
    }

    private void populateForbiddenClasses() {
        forbiddenPaths.add("java/");
        forbiddenPaths.add("java/");
        forbiddenPaths.add("javax/");
        forbiddenPaths.add("org/apache");
        forbiddenPaths.add("com/microsoft/applicationinsights");
        forbiddenPaths.add("sun/nio/");
        forbiddenPaths.add("sun/rmi/");
        forbiddenPaths.add("com/sun/jmx/");
        forbiddenPaths.add("sun/net/www/http/KeepAlive");
        forbiddenPaths.add("com.google");
    }

    private void populateSqlClasses() {
        sqlClasses.add("com/mysql/jdbc/StatementImpl");
        sqlClasses.add("com/mysql/jdbc/PreparedStatement");
        sqlClasses.add("com/mysql/jdbc/ServerPreparedStatement");
        sqlClasses.add("com/mysql/jdbc/CallableStatement");
        sqlClasses.add("com/mysql/jdbc/JDBC4CallableStatement");
        sqlClasses.add("com/mysql/jdbc/JDBC4PreparedStatement");
        sqlClasses.add("com/mysql/jdbc/JDBC4ServerPreparedStatement");
        sqlClasses.add("com/mysql/jdbc/jdbc2/optional/StatementWrapper");
        sqlClasses.add("com/mysql/jdbc/jdbc2/optional/JDBC4StatementWrapper");
        sqlClasses.add("com/mysql/jdbc/jdbc2/optional/CallableStatementWrapper");
        sqlClasses.add("com/mysql/jdbc/jdbc2/optional/JDBC4PreparedStatementWrapper");

        sqlClasses.add("org/sqlite/jdbc4/JDBC4Statement");
        sqlClasses.add("org/sqlite/core/CorePreparedStatement");
        sqlClasses.add("org/sqlite/jdbc3/JDBC3PreparedStatement");
        sqlClasses.add("org/sqlite/jdbc4/JDBC4PreparedStatement");

        sqlClasses.add("org/hsqldb/jdbc/JDBCPreparedStatement");
        sqlClasses.add("org/hsqldb/jdbc/jdbcCallableStatement");
        sqlClasses.add("org/hsqldb/jdbc/JDBCStatement");

        sqlClasses.add("org/postgresql/core/BaseStatement");
        sqlClasses.add("org/postgresql/jdbc2/AbstractJdbc2Statement");
        sqlClasses.add("org/postgresql/jdbc3g/AbstractJdbc3gStatement");
        sqlClasses.add("org/postgresql/jdbc4/AbstractJdbc4Statement");
        sqlClasses.add("org/postgresql/jdbc4/Jdbc4Statement");
        sqlClasses.add("org/postgresql/jdbc4/Jdbc4PreparedStatement");
        sqlClasses.add("org/postgresql/jdbc4/Jdbc4CallableStatement");

        HashSet<String> methodNamesOnly = new HashSet<String>();
        methodNamesOnly.add("execute");
        methodNamesOnly.add("executeQuery");

        for (String className : sqlClasses) {
            addToClasses(className, InstrumentedClassType.SQL, methodNamesOnly);
        }
    }

    private void populateHttpClasses() {
        httpClasses.add("sun/net/www/protocol/http/HttpURLConnection$HttpInputStream");

        ClassInstrumentationData data =
                new ClassInstrumentationData("sun/net/www/protocol/http/HttpURLConnection$HttpInputStream", InstrumentedClassType.HTTP)
                    .setReportCaughtExceptions(false)
                    .setReportExecutionTime(true);
        data.addMethod("read", "([BII)I", false, true);

        classesToInstrument.put(data.className, data);
    }

    private boolean isForbidden(String className) {
        for (String f : forbiddenPaths) {
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

        builtInEnabled = agentConfiguration.getBuiltInSwitches().isEnabled();

        HashMap<String, ClassInstrumentationData> configurationData = agentConfiguration.getRequestedClassesToInstrument();
        if (configurationData != null) {
            for (ClassInstrumentationData classInstrumentationData : configurationData.values()) {
                if (isForbidden(classInstrumentationData.className)) {
                    InternalAgentLogger.INSTANCE.trace("'%s' is not added since it is not allowed", classInstrumentationData.className);
                    continue;
                }

                InternalAgentLogger.INSTANCE.trace("Adding '%s'", classInstrumentationData.className);
                classesToInstrument.put(classInstrumentationData.className, classInstrumentationData);
            }
        }
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
