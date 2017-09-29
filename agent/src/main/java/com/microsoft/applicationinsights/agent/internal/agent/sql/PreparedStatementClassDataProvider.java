/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.agent.internal.agent.sql;

import java.util.*;

import com.microsoft.applicationinsights.agent.internal.agent.*;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by gupele on 8/3/2015.
 */
public final class PreparedStatementClassDataProvider {
    private final Map<String, ClassInstrumentationData> classesToInstrument;

    public PreparedStatementClassDataProvider(Map<String, ClassInstrumentationData> classesToInstrument) {
        this.classesToInstrument = classesToInstrument;
    }

    public void add() {
        try {
            ClassVisitorFactory factory = classFactoryForDerbyDB();
            doAdd(factory, "org/apache/derby/client/am/PreparedStatement");
            doAdd(factory, "org/apache/derby/client/am/ClientPreparedStatement");

            factory = classFactoryForHSQLDB();
            doAdd(factory, "org/hsqldb/jdbc/JDBCPreparedStatement");

            factory = classFactoryForMySql();
            doAdd(factory, "com/mysql/jdbc/PreparedStatement");

            factory = classFactoryForPostgreSql();
            doAdd(factory, "org/postgresql/jdbc2/AbstractJdbc2Statement");

            factory = classFactoryForOracle();
            doAdd(factory, "oracle/jdbc/driver/OraclePreparedStatement");

            factory = classFactoryForSqlServer();
            doAdd(factory, "com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement");

            addSqlite();
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            InternalAgentLogger.INSTANCE.error("Exception while loading HTTP classes: '%s'", t.getMessage());
        }
    }

    private void doAdd(ClassVisitorFactory classVisitorFactory, String className) {

        MethodVisitorFactory methodVisitorFactory = new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision,
                                        int access,
                                        String desc,
                                        String owner,
                                        String methodName,
                                        MethodVisitor methodVisitor,
                                        ClassToMethodTransformationData additionalData) {
                if (methodName.equals("executeBatch")) {
                    return new PreparedStatementMethodForExecuteBatchVisitor(access, desc, owner, methodName, methodVisitor, null);
                } else if (methodName.equals("clearParameters")) {
                    return new PreparedStatementForClearParametersMethodVisitor(access, desc, owner, methodName, methodVisitor, null);
                } else if (methodName.equals("clearBatch")) {
                    return new PreparedStatementForClearBatchMethodVisitor(access, desc, owner, methodName, methodVisitor, null);
                } else if (methodName.equals("addBatch")) {
                    return new PreparedStatementForAddBatchMethodVisitor(access, desc, owner, methodName, methodVisitor, null);
                } else if (methodName.equals("executeQuery")) {
                    if (desc.equals("()Ljava/sql/ResultSet;")) {
                        return new PreparedStatementMethodVisitor(access, desc, owner, methodName, methodVisitor, null);
                    } else {
                        return new QueryStatementWithPossibleExplainMethodVisitor(access, desc, owner, methodName, methodVisitor);
                    }
                } else if (methodName.equals("execute")) {
                    if (desc.equals("()Z")) {
                        return new PreparedStatementMethodVisitor(access, desc, owner, methodName, methodVisitor, null);
                    } else {
                        return new StatementMethodVisitor(access, desc, owner, methodName, methodVisitor);
                    }
                } else if (methodName.equals("executeUpdate")) {
                    if (desc.equals("()I")) {
                        return new PreparedStatementMethodVisitor(access, desc, owner, methodName, methodVisitor, null);
                    } else {
                        return new StatementMethodVisitor(access, desc, owner, methodName, methodVisitor);
                    }
                }

                // StatementMethodVisitor
                return new PreparedStatementMethodVisitor(access, desc, owner, methodName, methodVisitor, null);
            }
        };

        final HashMap<String, String> sqlStatementSignatures = new HashMap<String, String>();
        sqlStatementSignatures.put("executeBatch", "()[I");
        sqlStatementSignatures.put("execute", "()Z");
        sqlStatementSignatures.put("executeUpdate", "()I");
        sqlStatementSignatures.put("executeQuery", "()Ljava/sql/ResultSet;");
        sqlStatementSignatures.put("clearParameters", "()V");
        sqlStatementSignatures.put("clearBatch", "()V");
        sqlStatementSignatures.put("addBatch", "()V");

        ClassInstrumentationData data =
                new ClassInstrumentationData(className, InstrumentedClassType.SQL, classVisitorFactory)
                        .setReportCaughtExceptions(false)
                        .setReportExecutionTime(true);
        for (Map.Entry<String, String> methodAndSignature : sqlStatementSignatures.entrySet()) {
            data.addMethod(methodAndSignature.getKey(), methodAndSignature.getValue(), false, true, 0, methodVisitorFactory);
        }

        classesToInstrument.put(className, data);

        final HashMap<String, List<String>> sqlSignatures = new HashMap<String, List<String>>();
        ArrayList<String> signatures = new ArrayList<String>();

        signatures.add("(Ljava/lang/String;)Ljava/sql/ResultSet;");
        sqlSignatures.put("executeQuery", signatures);

        signatures = new ArrayList<String>();
        signatures.add("(Ljava/lang/String;I)I");
        signatures.add("(Ljava/lang/String;[I)I");
        signatures.add("(Ljava/lang/String;[Ljava/lang/String;)I");
        sqlSignatures.put("executeUpdate", signatures);

        signatures = new ArrayList<String>();
        signatures.add("(Ljava/lang/String;)Z");
        signatures.add("((Ljava/lang/String;I)Z");
        signatures.add("(Ljava/lang/String;[I)Z");
        signatures.add("(Ljava/lang/String;[Ljava/lang/String;)Z");
        sqlSignatures.put("execute", signatures);

        for (Map.Entry<String, List<String>> methodAndSignature : sqlSignatures.entrySet()) {
            for (String signature : methodAndSignature.getValue()) {
                data.addMethod(methodAndSignature.getKey(), signature, false, true, 0, methodVisitorFactory);
            }
        }
    }

    private ClassVisitorFactory classFactoryForHSQLDB() {
        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                HashSet<String> ctorSignatures = new HashSet<String>();
                ctorSignatures.add("(Lorg/hsqldb/jdbc/JDBCConnection;Ljava/lang/String;IIII[I[Ljava/lang/String;)V");
                final PreparedStatementMetaData metaData = new PreparedStatementMetaData(ctorSignatures);
                metaData.sqlStringInCtor = 2;
                return new PreparedStatementClassVisitor(classInstrumentationData, classWriter, metaData);
            }
        };

        return classVisitorFactory;
    }

    private ClassVisitorFactory classFactoryForDerbyDB() {
        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                HashSet<String> ctorSignatures = new HashSet<String>();
                ctorSignatures.add("(Lorg/apache/derby/client/am/Agent;Lorg/apache/derby/client/am/Connection;Ljava/lang/String;Lorg/apache/derby/client/am/Section;Lorg/apache/derby/client/ClientPooledConnection;)V");
                ctorSignatures.add("(Lorg/apache/derby/client/am/Agent;Lorg/apache/derby/client/am/Connection;Ljava/lang/String;IIII[Ljava/lang/String;[ILorg/apache/derby/client/ClientPooledConnection;)V");
                final PreparedStatementMetaData metaData = new PreparedStatementMetaData(ctorSignatures);
                metaData.sqlStringInCtor = 3;
                return new PreparedStatementClassVisitor(classInstrumentationData, classWriter, metaData);
            }
        };

        return classVisitorFactory;
    }

    private ClassVisitorFactory classFactoryForMySql() {

        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                HashSet<String> ctorSignatures = new HashSet<String>();
                ctorSignatures.add("(Lcom/mysql/jdbc/MySQLConnection;Ljava/lang/String;)V");
                ctorSignatures.add("(Lcom/mysql/jdbc/MySQLConnection;Ljava/lang/String;Ljava/lang/String;)V");
                final PreparedStatementMetaData metaData1 = new PreparedStatementMetaData(ctorSignatures);
                metaData1.sqlStringInCtor = 2;
                return new PreparedStatementClassVisitor(classInstrumentationData, classWriter, metaData1);
            }
        };

        return classVisitorFactory;
    }

    private ClassVisitorFactory classFactoryForPostgreSql() {

        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                HashSet<String> ctorSignatures = new HashSet<String>();
                ctorSignatures.add("(Lorg/postgresql/jdbc2/AbstractJdbc2Connection;Ljava/lang/String;ZII)V");
                final PreparedStatementMetaData metaData1 = new PreparedStatementMetaData(ctorSignatures);
                metaData1.sqlStringInCtor = 2;
                return new PreparedStatementClassVisitor(classInstrumentationData, classWriter, metaData1);
            }
        };

        return classVisitorFactory;
    }

    private ClassVisitorFactory classFactoryForOracle() {

        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                HashSet<String> ctorSignatures = new HashSet<String>();
                ctorSignatures.add("(Loracle/jdbc/driver/PhysicalConnection;Ljava/lang/String;II)V");
                ctorSignatures.add("(Loracle/jdbc/driver/PhysicalConnection;Ljava/lang/String;IIII)V");
                final PreparedStatementMetaData metaData1 = new PreparedStatementMetaData(ctorSignatures);
                metaData1.sqlStringInCtor = 2;
                return new PreparedStatementClassVisitor(classInstrumentationData, classWriter, metaData1);
            }
        };

        return classVisitorFactory;
    }

    private ClassVisitorFactory classFactoryForSqlServer() {

        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                HashSet<String> ctorSignatures = new HashSet<String>();
                ctorSignatures.add("(Lcom/microsoft/sqlserver/jdbc/SQLServerConnection;Ljava/lang/String;II)V");
                final PreparedStatementMetaData metaData1 = new PreparedStatementMetaData(ctorSignatures);
                metaData1.sqlStringInCtor = 2;
                return new PreparedStatementClassVisitor(classInstrumentationData, classWriter, metaData1);
            }
        };

        return classVisitorFactory;
    }

    private void addSqlite() {

        ClassVisitorFactory classVisitorFactory = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
                HashSet<String> ctorSignatures = new HashSet<String>();
                ctorSignatures.add("(Lorg/sqlite/SQLiteConnection;Ljava/lang/String;)V");
                final PreparedStatementMetaData metaData1 = new PreparedStatementMetaData(ctorSignatures);
                metaData1.sqlStringInCtor = 2;
                return new PreparedStatementClassVisitor(classInstrumentationData, classWriter, metaData1);
            }
        };

        String className = "org/sqlite/core/CorePreparedStatement";
        ClassInstrumentationData data =
                new ClassInstrumentationData(className, InstrumentedClassType.SQL, classVisitorFactory)
                        .setReportCaughtExceptions(false)
                        .setReportExecutionTime(true);
        data.addMethod("executeBatch", "()[I", false, true, 0, new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision, int access, String desc, String owner, String methodName, MethodVisitor methodVisitor, ClassToMethodTransformationData additionalData) {
                return new PreparedStatementMethodForExecuteBatchVisitor(access, desc, owner, methodName, methodVisitor, null);
            }
        });
        classesToInstrument.put(className, data);

        doAdd(null, "org/sqlite/jdbc3/JDBC3PreparedStatement");
    }
}

