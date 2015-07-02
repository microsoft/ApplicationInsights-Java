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

package com.microsoft.applicationinsights.internal.agent;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import com.microsoft.applicationinsights.agent.internal.coresync.AgentNotificationsHandler;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.schemav2.DependencyKind;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;

/**
 * The Core's implementation: the methods are called for instrumented methods.
 * The implementation can measure time in nano seconds, fetch Sql/Http data and report exceptions
 *
 * Created by gupele on 5/7/2015.
 */
final class CoreAgentNotificationsHandler implements AgentNotificationsHandler {

    /**
     * The class holds the data gathered on a method
     */
    private static class MethodData {
        public String name;
        public String[] arguments;
        public long interval;
        public InstrumentedClassType type;
        public Object result;
    }

    private static class ThreadData {
        public final LinkedList<MethodData> methods = new LinkedList<MethodData>();
    }

    static final class ThreadLocalData extends ThreadLocal<ThreadData> {
        private ThreadData threadData;

        @Override
        protected ThreadData initialValue() {
            threadData = new ThreadData();
            return threadData;
        }
    };

    private ThreadLocalData threadDataThreadLocal = new ThreadLocalData();

    private TelemetryClient telemetryClient = new TelemetryClient();

    private final String name;

    public CoreAgentNotificationsHandler(String name) {
        this.name = name;
    }

    @Override
    public void onThrowable(String classAndMethodNames, Throwable throwable) {
        try {
            if (throwable instanceof Exception) {
                telemetryClient.trackException((Exception)throwable);
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public void onMethodEnterURL(String name, URL url) {
        String urlAsString = (url == null) ? null : url.toString();
        startMethod(InstrumentedClassType.HTTP, name, urlAsString);
    }

    @Override
    public void onMethodEnterSqlStatement(String name, Statement statement, String sqlStatement) {
        if (statement == null) {
            return;
        }

        try {
            Connection connection = statement.getConnection();
            if (connection == null) {
                return;
            }

            DatabaseMetaData metaData = connection.getMetaData();
            if (metaData == null) {
                return;
            }

            String url = metaData.getURL();
            startMethod(InstrumentedClassType.SQL, name, url, sqlStatement);

        } catch (SQLException e) {
        }
    }

//    @Override
    public String getName() {
        return name;
    }

    @Override
    public void onMethodEnter(String name) {
        startMethod(InstrumentedClassType.OTHER, name, new String[]{});
    }

    @Override
    public void onMethodFinish(String name, Throwable throwable) {
        finalizeMethod(null, true);
    }

    @Override
    public void onMethodFinish(String name) {
        finalizeMethod(null, false);
    }

    private void startMethod(InstrumentedClassType type, String name, String... arguments) {
        long start = System.nanoTime();

        ThreadData localData = threadDataThreadLocal.get();
        MethodData methodData = new MethodData();
        methodData.interval = start;
        methodData.type = type;
        methodData.arguments = arguments;
        methodData.name = name;
        localData.methods.addFirst(methodData);
    }

    private void finalizeMethod(Object result, boolean isException) {
        long finish = System.nanoTime();

        ThreadData localData = threadDataThreadLocal.get();
        if (localData.methods == null || localData.methods.isEmpty()) {
            InternalLogger.INSTANCE.error("Agent has detected a 'Finish' method event without a 'Start'");
            return;
        }

        MethodData methodData = localData.methods.removeFirst();
        if (methodData == null) {
            return;
        }

        methodData.interval = finish - methodData.interval;
        methodData.result = result;

        report(methodData, isException);
    }

    private void report(MethodData methodData, boolean isException) {

        switch (methodData.type) {
            case SQL:
                sendSQLTelemetry(methodData, isException);
                break;

            case HTTP:
                sendHTTPTelemetry(methodData, isException);
                break;

            default:
                sendInstrumentationTelemetry(methodData, isException);
                break;
        }
    }

    private void sendInstrumentationTelemetry(MethodData methodData, boolean isException) {
        Duration duration = new Duration(nanoToMilliseconds(methodData.interval));
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry(methodData.name, null, duration, !isException);
        telemetry.setDependencyKind(DependencyKind.Undefined);

        InternalLogger.INSTANCE.trace("Sending RDD event for '%s'", methodData.name);

        telemetryClient.track(telemetry);
    }

    private void sendHTTPTelemetry(MethodData methodData, boolean isException) {
        if (methodData.arguments != null && methodData.arguments.length == 1) {
            String url = methodData.arguments[0];
            Duration duration = new Duration(nanoToMilliseconds(methodData.interval));

            InternalLogger.INSTANCE.trace("Sending HTTP RDD event, URL: '%s'", url);

            RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry(url, null, duration, !isException);
            telemetryClient.trackDependency(telemetry);
        }
    }

    private void sendSQLTelemetry(MethodData methodData, boolean isException) {
        if (methodData.arguments != null && methodData.arguments.length == 2) {
            String dependencyName = methodData.arguments[0];
            String commandName = methodData.arguments[1];
            Duration duration = new Duration(nanoToMilliseconds(methodData.interval));

            InternalLogger.INSTANCE.trace("Sending Sql RDD event for '%s', command: '%s'", dependencyName, commandName);

            RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry(dependencyName, commandName, duration, !isException);
            telemetryClient.track(telemetry);
        }
    }

    private static long nanoToMilliseconds(long nanoSeconds) {
        return nanoSeconds / 1000000;
    }
}
