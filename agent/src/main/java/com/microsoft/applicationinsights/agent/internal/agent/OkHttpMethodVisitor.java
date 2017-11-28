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

package com.microsoft.applicationinsights.agent.internal.agent;

import java.net.URL;

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Created by gupele on 7/27/2015.
 */
public final class OkHttpMethodVisitor extends AbstractHttpMethodVisitor {
    private final String fieldName;

    public OkHttpMethodVisitor(int access,
                               String desc,
                               String owner,
                               String methodName,
                               MethodVisitor methodVisitor,
                               ClassToMethodTransformationData additionalData) {
        super(access, desc, owner, methodName, methodVisitor, additionalData);

        this.fieldName = ((OkHttpClassToMethodTransformationData)additionalData).fieldName;
    }

    @Override
    public void onMethodEnter() {

        int urlLocalIndex = this.newLocal(Type.getType(URL.class));
        int stringLocalIndex = this.newLocal(Type.getType(String.class));

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/squareup/okhttp/Call", fieldName, "Lcom/squareup/okhttp/Request;");

        Label nullLabel = new Label();
        mv.visitJumpInsn(IFNULL, nullLabel);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/squareup/okhttp/Call", fieldName, "Lcom/squareup/okhttp/Request;");

        mv.visitMethodInsn(INVOKEVIRTUAL, "com/squareup/okhttp/Request", "url", "()Ljava/net/URL;", false);
        mv.visitVarInsn(ASTORE, urlLocalIndex);

        mv.visitVarInsn(ALOAD, urlLocalIndex);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, stringLocalIndex);

        // generate child ID
        mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/web/internal/correlation/TelemetryCorrelationUtils", "generateChildDependencyId", "()Ljava/lang/String;", false);
        int childIdLocal = this.newLocal(Type.getType(Object.class));
        mv.visitVarInsn(ASTORE, childIdLocal);

        // retrieve correlation context
        mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/web/internal/correlation/TelemetryCorrelationUtils", "retrieveCorrelationContext", "()Ljava/lang/String;", false);
        int correlationContextLocal = this.newLocal(Type.getType(Object.class));
        mv.visitVarInsn(ASTORE, correlationContextLocal);
        
        // retrieve request context
        mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/web/internal/correlation/TelemetryCorrelationUtils", "retrieveApplicationCorrelationId", "()Ljava/lang/String;", false);
        int appCorrelationId = this.newLocal(Type.getType(Object.class));
        mv.visitVarInsn(ASTORE, appCorrelationId);

        // this.originalRequest
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/squareup/okhttp/Call", fieldName, "Lcom/squareup/okhttp/Request;");

        // get a new request builder
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/squareup/okhttp/Request", "newBuilder", "()Lcom/squareup/okhttp/Request$Builder;", false);
        int reqBuilderLocal = this.newLocal(Type.getType(Object.class));
        mv.visitVarInsn(ASTORE, reqBuilderLocal);

        // add headers
        mv.visitVarInsn(ALOAD, reqBuilderLocal);
        mv.visitLdcInsn("Request-Id");
        mv.visitVarInsn(ALOAD, childIdLocal);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/squareup/okhttp/Request$Builder", "addHeader", "(Ljava/lang/String;Ljava/lang/String;)Lcom/squareup/okhttp/Request$Builder;", false);

        mv.visitVarInsn(ALOAD, reqBuilderLocal);
        mv.visitLdcInsn("Correlation-Context");
        mv.visitVarInsn(ALOAD, correlationContextLocal);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/squareup/okhttp/Request$Builder", "addHeader", "(Ljava/lang/String;Ljava/lang/String;)Lcom/squareup/okhttp/Request$Builder;", false);

        mv.visitVarInsn(ALOAD, reqBuilderLocal);
        mv.visitLdcInsn("Request-Context");
        mv.visitVarInsn(ALOAD, appCorrelationId);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/squareup/okhttp/Request$Builder", "addHeader", "(Ljava/lang/String;Ljava/lang/String;)Lcom/squareup/okhttp/Request$Builder;", false);

        // build new request
        mv.visitVarInsn(ALOAD, reqBuilderLocal);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/squareup/okhttp/Request$Builder", "build", "()Lcom/squareup/okhttp/Request;", false);
        int newRequestLocal = this.newLocal(Type.getType(Object.class)); 
        mv.visitVarInsn(ASTORE, newRequestLocal);

        // re-assign original request to new one
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, newRequestLocal);
        mv.visitFieldInsn(PUTFIELD, "com/squareup/okhttp/Call", fieldName, "Lcom/squareup/okhttp/Request;");

        super.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);

        mv.visitLdcInsn(getMethodName());
        mv.visitVarInsn(ALOAD, stringLocalIndex);

        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, ON_ENTER_METHOD_NAME, ON_ENTER_METHOD_SIGNATURE, false);

        Label notNullLabel = new Label();
        mv.visitJumpInsn(GOTO, notNullLabel);

        mv.visitLabel(nullLabel);
        super.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);
        mv.visitLdcInsn(getMethodName());
        mv.visitInsn(ACONST_NULL);

        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, ON_ENTER_METHOD_NAME, ON_ENTER_METHOD_SIGNATURE, false);

        mv.visitLabel(notNullLabel);
    }
}

