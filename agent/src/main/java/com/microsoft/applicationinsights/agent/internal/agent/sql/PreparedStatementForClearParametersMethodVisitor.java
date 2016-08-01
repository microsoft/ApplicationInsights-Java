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

import com.microsoft.applicationinsights.agent.internal.agent.ClassToMethodTransformationData;
import com.microsoft.applicationinsights.agent.internal.agent.DefaultMethodVisitor;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by gupele on 8/5/2015.
 */
final class PreparedStatementForClearParametersMethodVisitor extends DefaultMethodVisitor {

    public PreparedStatementForClearParametersMethodVisitor(int access,
                                                            String desc,
                                                            String owner,
                                                            String methodName,
                                                            MethodVisitor methodVisitor,
                                                            ClassToMethodTransformationData additionalData) {
        super(false, true, 0, access, desc, owner, methodName, methodVisitor, null);
    }

    @Override
    protected void onMethodEnter() {
        int localIndex = this.newLocal(Type.getType(Integer.class));

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        Label l0 = new Label();
        mv.visitJumpInsn(IFNULL, l0);

        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, localIndex);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitFrame(Opcodes.F_APPEND, localIndex, new Object[] {Opcodes.INTEGER}, 0, null);
        mv.visitVarInsn(ILOAD, localIndex);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPGE, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, localIndex);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(AASTORE);
        mv.visitIincInsn(localIndex, 1);
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l0);
    }

    @Override
    protected void byteCodeForMethodExit(int opcode) {
    }
}
