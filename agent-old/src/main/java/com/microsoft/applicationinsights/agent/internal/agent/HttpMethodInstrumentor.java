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

import java.net.URL;

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * The class is responsible for instrumenting the default Java class that is responsible for sending HTTP requests
 *
 * Created by gupele on 5/20/2015.
 */
final class HttpMethodInstrumentor extends DefaultMethodVisitor {
    private final static String PARENT_NAME_IN_INSTANCE = "this$0";
    private final static String PARENT_JAVA_NAME = "sun/net/www/protocol/http/HttpURLConnection";
    private final static String PARENT_FULL_JAVA_NAME = "Lsun/net/www/protocol/http/HttpURLConnection;";
    private final static String GET_URL_METHOD_NAME = "getURL";
    private final static String GET_URL_METHOD_SIGNATURE = "()Ljava/net/URL;";

    private final static String ON_ENTER_METHOD_NAME = "httpMethodStarted";
    private final static String ON_ENTER_METHOD_SIGNATURE = "(Ljava/lang/String;Ljava/net/URL;)V";

    private final String owner;

    public HttpMethodInstrumentor(int access,
                                  String desc,
                                  String owner,
                                  String methodName,
                                  MethodVisitor methodVisitor,
                                  ClassToMethodTransformationData additionalData) {
        super(false, true, 0, access, desc, owner, methodName, methodVisitor, additionalData);
        this.owner = owner;
    }

    @Override
    protected void onMethodEnter() {
        int urlLocalIndex = this.newLocal(Type.getType(URL.class));

        // "sun/net/www/protocol/http/HttpURLConnection"
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, PARENT_NAME_IN_INSTANCE, PARENT_FULL_JAVA_NAME);
        mv.visitMethodInsn(INVOKEVIRTUAL, PARENT_JAVA_NAME, GET_URL_METHOD_NAME, GET_URL_METHOD_SIGNATURE, false);
        mv.visitVarInsn(ASTORE, urlLocalIndex);

        mv.visitVarInsn(ALOAD, urlLocalIndex);

        activateEnumMethod(
                ImplementationsCoordinator.class,
                ON_ENTER_METHOD_NAME,
                ON_ENTER_METHOD_SIGNATURE,
                getMethodName(),
                duplicateTopStackToTempVariable(Type.getType(URL.class)));
    }
}
