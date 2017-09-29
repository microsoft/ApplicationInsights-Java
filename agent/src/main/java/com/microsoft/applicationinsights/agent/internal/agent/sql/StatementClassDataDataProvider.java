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
import org.objectweb.asm.MethodVisitor;

/**
 * Created by gupele on 8/3/2015.
 */
public class StatementClassDataDataProvider {
    private final static String[] JDBC_STATEMENT_WITH_POSSIBLE_EXPLAIN_CLASS_NAMES = new String[] {
            "com/mysql/jdbc/StatementImpl"
    };

    private final static String[] JDBC_STATEMENT_CLASS_NAMES = new String[] {
            "org/hsqldb/jdbc/JDBCStatement",
            "oracle/jdbc/driver/OracleStatement",
            "com/microsoft/sqlserver/jdbc/SQLServerStatement",
            "org/apache/derby/client/am/Statement",
            "org/apache/derby/client/am/ClientStatement",
            "org/sqlite/jdbc3/JDBC3Statement"
    };

    private final Map<String, ClassInstrumentationData> classesToInstrument;
    private final HashMap<String, List<String>> sqlSignatures = new HashMap<String, List<String>>();

    public StatementClassDataDataProvider(Map<String, ClassInstrumentationData> classesToInstrument) {
        this.classesToInstrument = classesToInstrument;
    }


    public void add() {
        try {
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

            addStatements();
            addPossibleQueryLimit();
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            InternalAgentLogger.INSTANCE.error("Exception while loading HTTP classes: '%s'", t.getMessage());
        }
    }

    private void addPossibleQueryLimit() {
        MethodVisitorFactory methodVisitorFactory = new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision,
                                        int access,
                                        String desc,
                                        String owner,
                                        String methodName,
                                        MethodVisitor methodVisitor,
                                        ClassToMethodTransformationData additionalData) {
                if (methodName.equals("executeQuery")) {
                    return new QueryStatementWithPossibleExplainMethodVisitor(access, desc, owner, methodName, methodVisitor);
                }

                return new StatementMethodVisitor(access, desc, owner, methodName, methodVisitor);
            }
        };

        final HashSet<String> sqlClasses = new HashSet<String>(Arrays.asList(JDBC_STATEMENT_WITH_POSSIBLE_EXPLAIN_CLASS_NAMES));

        for (String className : sqlClasses) {
            ClassInstrumentationData data =
                    new ClassInstrumentationData(className, InstrumentedClassType.SQL)
                            .setReportCaughtExceptions(false)
                            .setReportExecutionTime(true);
            for (Map.Entry<String, List<String>> methodAndSignature : sqlSignatures.entrySet()) {
                for (String signature : methodAndSignature.getValue()) {
                    data.addMethod(methodAndSignature.getKey(), signature, false, true, 0, methodVisitorFactory);
                }
            }

            classesToInstrument.put(className, data);
        }
    }

    private void addStatements() {
        MethodVisitorFactory methodVisitorFactory = new MethodVisitorFactory() {
            @Override
            public MethodVisitor create(MethodInstrumentationDecision decision,
                                        int access,
                                        String desc,
                                        String owner,
                                        String methodName,
                                        MethodVisitor methodVisitor,
                                        ClassToMethodTransformationData additionalData) {
                return new StatementMethodVisitor(access, desc, owner, methodName, methodVisitor);
            }
        };

        final HashSet<String> sqlClasses = new HashSet<String>(Arrays.asList(JDBC_STATEMENT_CLASS_NAMES));

        for (String className : sqlClasses) {
            ClassInstrumentationData data =
                    new ClassInstrumentationData(className, InstrumentedClassType.SQL)
                            .setReportCaughtExceptions(false)
                            .setReportExecutionTime(true);
            for (Map.Entry<String, List<String>> methodAndSignature : sqlSignatures.entrySet()) {
                for (String signature : methodAndSignature.getValue()) {
                    data.addMethod(methodAndSignature.getKey(), signature, false, true, 0, methodVisitorFactory);
                }
            }

            classesToInstrument.put(className, data);
        }
    }
}
