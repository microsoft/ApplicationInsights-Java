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

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Created by gupele on 5/20/2015.
 */
final class EnterExitMethodForSqlStatement extends EnterExitMethodWrapper {
    private final static String ON_ENTER_METHOD_NANE = "onMethodEnterSqlStatement";
    private final static String ON_ENTER_METHOD_SIGNATURE = "(Ljava/lang/String;Ljava/sql/Statement;Ljava/lang/String;)V";

    private final String implementationCoordinatorInternalName;
    private final String implementationCoordinatorJavaName;

    public EnterExitMethodForSqlStatement(int access, String desc, String owner, String methodName, MethodVisitor methodVisitor) {
        super(false, true, access, desc, owner, methodName, methodVisitor);

        implementationCoordinatorInternalName = Type.getInternalName(ImplementationsCoordinator.class);
        implementationCoordinatorJavaName = "L" + implementationCoordinatorInternalName + ";";
    }

    @Override
    protected void onMethodEnter() {

        super.visitFieldInsn(GETSTATIC, implementationCoordinatorInternalName, "INSTANCE", implementationCoordinatorJavaName);

        mv.visitLdcInsn(getMethodName());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);

        mv.visitMethodInsn(INVOKEVIRTUAL, implementationCoordinatorInternalName, ON_ENTER_METHOD_NANE, ON_ENTER_METHOD_SIGNATURE, false);
    }
}
