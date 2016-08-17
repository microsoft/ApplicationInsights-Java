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

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Created by gupele on 7/27/2015.
 */
public final class HttpClientMethodVisitor extends AbstractHttpMethodVisitor {

    public HttpClientMethodVisitor(int access,
                                   String desc,
                                   String owner,
                                   String methodName,
                                   MethodVisitor methodVisitor,
                                   ClassToMethodTransformationData additionalData) {
        super(access, desc, owner, methodName, methodVisitor, additionalData);
    }

    @Override
    public void onMethodEnter() {
        mv.visitVarInsn(ALOAD, 2);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNULL, l0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/apache/http/HttpRequest", "getRequestLine", "()Lorg/apache/http/RequestLine;", true);
        mv.visitVarInsn(ASTORE, 4);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitMethodInsn(INVOKEINTERFACE, "http/RequestLine", "getUri", "()Ljava/lang/String;", true);
        mv.visitVarInsn(ASTORE, 5);

        mv.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);
        mv.visitLdcInsn(getMethodName());
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, ON_ENTER_METHOD_NAME, ON_ENTER_METHOD_SIGNATURE, false);

        mv.visitLabel(l0);
        mv.visitInsn(ARETURN);
    }
}
