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

import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by gupele on 5/11/2015.
 */
class DefaultClassDataProvider implements ClassDataProvider {

    private final static String HTTP_CLIENT_43_CLASS_NAME = "org/apache/http/impl/client/InternalHttpClient";
    private final static String HTTP_CLIENT_METHOD_43_NAME = "doExecute";
    private final static String HTTP_CLIENT_METHOD_43_SIGNATURE = "(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/client/methods/CloseableHttpResponse;";

    private final static String HTTP_CLIENT_42_CLASS_NAME = "org/apache/http/impl/client/AbstractHttpClient";
    private final static String HTTP_CLIENT_METHOD_42_NAME = "execute";
    private final static String HTTP_CLIENT_METHOD_42_SIGNATURE = "(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;";

    private final static String OK_HTTP_CLIENT_CALL_CLASS_NAME = "com/squareup/okhttp/Call";
    private final static String OK_HTTP_CLIENT_CALL_METHOD_NAME = "execute";
    private final static String OK_HTTP_CLIENT_CALL_METHOD_SIGNATURE = "()Lcom/squareup/okhttp/Response;";

    private final static String OK_HTTP_CLIENT_CALL_ASYNC_CLASS_NAME = "com/squareup/okhttp/Call$AsyncCall";
    private final static String OK_HTTP_CLIENT_CALL_ASYNC_METHOD_NAME = "execute";
    private final static String OK_HTTP_CLIENT_CALL_ASYNC_METHOD_SIGNATURE = "()V";

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

    private final HashSet<String> sqlClasses = new HashSet<String>();
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

            populateSqlClasses();
            populateHttpClasses();
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
    public ByteCodeTransformer getAndRemove(String className) {
        final ClassInstrumentationData classInstrumentationData = classesToInstrument.remove(className);
        if (classInstrumentationData == null) {
            return null;
        }

        return new ByteCodeTransformer(classInstrumentationData);
    }

    private void populateSqlClasses() {
        sqlClasses.addAll(Arrays.asList(JDBC_CLASS_NAMES));

        HashSet<String> methodNamesOnly = new HashSet<String>(Arrays.asList(JDBC_METHODS_TO_TRACK));

        MethodVisitorFactory methodVisitorFactory = new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision,
                                        int access,
                                        String desc,
                                        String owner,
                                        String methodName,
                                        MethodVisitor methodVisitor,
                                        ClassToMethodTransformationData additionalData) {
                return new SqlStatementMethodVisitor(access, desc, owner, methodName, methodVisitor);
            }
        };
        for (String className : sqlClasses) {
            ClassInstrumentationData data =
                    new ClassInstrumentationData(className, InstrumentedClassType.SQL)
                            .setReportCaughtExceptions(false)
                            .setReportExecutionTime(true);
            for (String methodName : methodNamesOnly) {
                data.addMethod(methodName, null, false, true, methodVisitorFactory);
            }
            classesToInstrument.put(className, data);
        }
    }

    private void populateHttpClasses() {
        MethodVisitorFactory methodVisitorFactory = new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision,
                                        int access,
                                        String desc,
                                        String className,
                                        String methodName,
                                        MethodVisitor methodVisitor,
                                        ClassToMethodTransformationData additionalData) {
                return new HttpClientMethodVisitor(access, desc, className, methodName, methodVisitor, additionalData);
            }
        };

        addToHttpClasses(null,
                         methodVisitorFactory,
                         InstrumentedClassType.HTTP,
                         HTTP_CLIENT_43_CLASS_NAME,
                         HTTP_CLIENT_METHOD_43_NAME,
                         HTTP_CLIENT_METHOD_43_SIGNATURE);
        addToHttpClasses(null,
                         methodVisitorFactory,
                         InstrumentedClassType.HTTP,
                         HTTP_CLIENT_42_CLASS_NAME,
                         HTTP_CLIENT_METHOD_42_NAME,
                         HTTP_CLIENT_METHOD_42_SIGNATURE);

        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public DefaultClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                return new OkHttpClassVisitor(classInstrumentationData, classWriter);
            }
        };

        methodVisitorFactory = new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision,
                                        int access,
                                        String desc,
                                        String className,
                                        String methodName,
                                        MethodVisitor methodVisitor,
                                        ClassToMethodTransformationData additionalData) {
                return new OkHttpMethodVisitor(access, desc, className, methodName, methodVisitor, additionalData);
            }
        };
        addToHttpClasses(classVisitorFactory,
                         methodVisitorFactory,
                         InstrumentedClassType.HTTP,
                         OK_HTTP_CLIENT_CALL_CLASS_NAME,
                         OK_HTTP_CLIENT_CALL_METHOD_NAME,
                         OK_HTTP_CLIENT_CALL_METHOD_SIGNATURE);

        methodVisitorFactory = new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision,
                                        int access,
                                        String desc,
                                        String className,
                                        String methodName,
                                        MethodVisitor methodVisitor,
                                        ClassToMethodTransformationData additionalData) {
                return new OkHttpAsyncCallMethodVisitor(access, desc, className, methodName, methodVisitor, additionalData);
            }
        };
        addToHttpClasses(classVisitorFactory,
                         methodVisitorFactory,
                         InstrumentedClassType.HTTP,
                         OK_HTTP_CLIENT_CALL_ASYNC_CLASS_NAME,
                         OK_HTTP_CLIENT_CALL_ASYNC_METHOD_NAME,
                         OK_HTTP_CLIENT_CALL_ASYNC_METHOD_SIGNATURE);
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

    private void addToHttpClasses(ClassVisitorFactory classVisitorFactory,
                                  MethodVisitorFactory methodVisitorFactory,
                                  InstrumentedClassType type,
                                  String className,
                                  String methodName,
                                  String methodSignature) {
        ClassInstrumentationData data =
                new ClassInstrumentationData(className, type, classVisitorFactory)
                        .setReportCaughtExceptions(false)
                        .setReportExecutionTime(true);
        data.addMethod(methodName, methodSignature, false, true, methodVisitorFactory);

        classesToInstrument.put(className, data);
    }
}
