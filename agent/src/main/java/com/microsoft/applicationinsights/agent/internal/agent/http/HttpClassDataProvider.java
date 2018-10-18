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

package com.microsoft.applicationinsights.agent.internal.agent.http;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.agent.ClassToMethodTransformationData;
import com.microsoft.applicationinsights.agent.internal.agent.ClassVisitorFactory;
import com.microsoft.applicationinsights.agent.internal.agent.HttpClientMethodVisitor;
import com.microsoft.applicationinsights.agent.internal.agent.MethodInstrumentationDecision;
import com.microsoft.applicationinsights.agent.internal.agent.MethodVisitorFactory;
import com.microsoft.applicationinsights.agent.internal.agent.OkHttpAsyncCallMethodVisitor;
import com.microsoft.applicationinsights.agent.internal.agent.OkHttpClassVisitor;
import com.microsoft.applicationinsights.agent.internal.agent.OkHttpMethodVisitor;
import com.microsoft.applicationinsights.agent.internal.agent.RestTemplateMethodVisitor;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by gupele on 8/3/2015.
 */
public final class HttpClassDataProvider {
    private final static String HTTP_CLIENT_43_CLASS_NAME = "org/apache/http/impl/client/InternalHttpClient";
    private final static String HTTP_CLIENT_METHOD_43_NAME = "doExecute";
    private final static String HTTP_CLIENT_METHOD_43_SIGNATURE = "(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/client/methods/CloseableHttpResponse;";

    private final static String HTTP_CLIENT_42_CLASS_NAME = "org/apache/http/impl/client/AbstractHttpClient";
    private final static String HTTP_CLIENT_METHOD_42_NAME = "execute";
    private final static String HTTP_CLIENT_METHOD_42_SIGNATURE = "(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;";

    private final static String OK_HTTP_CLIENT_CALL_CLASS_NAME = "com/squareup/okhttp/Call";
    private final static String OK_HTTP_CLIENT_CALL_METHOD_NAME = "execute";
    private final static String OK_HTTP_CLIENT_CALL_METHOD_SIGNATURE = "()Lcom/squareup/okhttp/Response;";

    private final static String REST_TEMPLATE_CLASS_NAME = "org/springframework/web/client/RestTemplate";

    private final static String OK_HTTP_CLIENT_CALL_ASYNC_CLASS_NAME = "com/squareup/okhttp/Call$AsyncCall";
    private final static String OK_HTTP_CLIENT_CALL_ASYNC_METHOD_NAME = "execute";
    private final static String OK_HTTP_CLIENT_CALL_ASYNC_METHOD_SIGNATURE = "()V";

    private final static String REST_TEMPLATE_METTHOD = "doExecute";

    private final Map<String, ClassInstrumentationData> classesToInstrument;

    public HttpClassDataProvider(Map<String, ClassInstrumentationData> classesToInstrument) {
        this.classesToInstrument = classesToInstrument;
    }

    public void add() {
        try {
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
                public ClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
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

            methodVisitorFactory = new MethodVisitorFactory() {
                @Override
                public MethodVisitor create(MethodInstrumentationDecision decision,
                                            int access,
                                            String desc,
                                            String className,
                                            String methodName,
                                            MethodVisitor methodVisitor,
                                            ClassToMethodTransformationData additionalData) {
                    return new RestTemplateMethodVisitor(access, desc, className, methodName, methodVisitor, additionalData);
                }
            };
            addToHttpClasses(classVisitorFactory,
                    methodVisitorFactory,
                    InstrumentedClassType.HTTP,
                    REST_TEMPLATE_CLASS_NAME,
                    REST_TEMPLATE_METTHOD,
                    null);
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalLogger.INSTANCE.error("Exception while loading HTTP classes: '%s'",
                    ExceptionUtils.getStackTrace(t));            }
                catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
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
        data.addMethod(methodName, methodSignature, false, true, 0, methodVisitorFactory);

        classesToInstrument.put(className, data);
    }
}
