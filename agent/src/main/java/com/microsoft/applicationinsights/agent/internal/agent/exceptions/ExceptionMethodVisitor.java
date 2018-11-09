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

package com.microsoft.applicationinsights.agent.internal.agent.exceptions;

import com.microsoft.applicationinsights.agent.internal.agent.AdvancedAdviceAdapter;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Created by gupele on 8/1/2016.
 */
public class ExceptionMethodVisitor extends AdvancedAdviceAdapter {
    private final static String RT_EXCEPTION_METHOD_NAME = "exceptionThrown";
    private final static String RT_EXCEPTION_METHOD_SIGNATURE = "(Ljava/lang/Exception;)V";

    protected ExceptionMethodVisitor(
            boolean reportExecutionTime,
            int access,
            String desc,
            String owner,
            String methodName,
            MethodVisitor methodVisitor) {
        super(reportExecutionTime, ASM5, methodVisitor, access, owner, methodName, desc);
    }

    @Override
    protected void onMethodExit(int opcode) {
        String internalName = Type.getInternalName(ImplementationsCoordinator.class);
        mv.visitFieldInsn(GETSTATIC, internalName, "INSTANCE", "L" + internalName + ";");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, internalName, RT_EXCEPTION_METHOD_NAME,RT_EXCEPTION_METHOD_SIGNATURE, false);
    }

    @Override
    protected void byteCodeForMethodExit(int opcode) {

    }
}
