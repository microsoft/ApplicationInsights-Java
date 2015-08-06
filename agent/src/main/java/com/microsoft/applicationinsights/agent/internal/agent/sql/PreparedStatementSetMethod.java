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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by gupele on 8/3/2015.
 */
final class PreparedStatementSetMethod extends DefaultMethodVisitor {

    protected final PreparedStatementMetaData metaData;
    private final String methodName;

    public PreparedStatementSetMethod(int access, String desc, String owner, String methodName, MethodVisitor methodVisitor, ClassToMethodTransformationData additionalData) {
        super(false, false, access, desc, owner, methodName, methodVisitor, additionalData);
        this.metaData = (PreparedStatementMetaData)additionalData;
        this.methodName = methodName;
    }

    @Override
    protected void onMethodEnter() {

        int localIndex = this.newLocal(Type.getType(Object.class));
        int tmpArrayIndex = this.newLocal(Type.getType(Object.class));

        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitVarInsn(ISTORE, localIndex);

        mv.visitVarInsn(ILOAD, localIndex);
        Label l0 = new Label();
        mv.visitJumpInsn(IFLT, l0);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        Label l1 = new Label();
        mv.visitJumpInsn(IFNONNULL, l1);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitFieldInsn(PUTFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");

        Label l2 = new Label();

        mv.visitJumpInsn(GOTO, l2);
        mv.visitLabel(l1);

        mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);

        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        mv.visitInsn(ARRAYLENGTH);

        mv.visitJumpInsn(IF_ICMPLE, l2);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        mv.visitVarInsn(ASTORE, tmpArrayIndex);

        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitFieldInsn(PUTFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");

        mv.visitVarInsn(ALOAD, tmpArrayIndex);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, tmpArrayIndex);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);

        mv.visitLabel(l2);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, localIndex);
        addArgument();
        mv.visitInsn(AASTORE);

        mv.visitLabel(l0);
    }

    @Override
    protected void byteCodeForMethodExit(int opcode) {
    }

    private void addArgument() {
        if ("setBoolean".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(I)Ljava/lang/Boolean;", false);
        } else if ("setInt".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if ("setLong".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(I)Ljava/lang/Long;", false);
        } else if ("setShort".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(I)Ljava/lang/short;", false);
        } else if ("setDouble".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(I)Ljava/lang/double;", false);
        } else if ("setFloat".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(I)Ljava/lang/Float;", false);
        } else if ("setBigDecimal".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
        } else if ("setString".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
        } else if ("setTimestamp".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
        } else if ("setTime".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
        } else if ("setDate".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
        } else if ("setBlob".equals(methodName)) {
            mv.visitLdcInsn("BLOB");
        } else if ("setNull".equals(methodName)) {
            mv.visitLdcInsn("null");
        } else {
            mv.visitLdcInsn("UNKNOWN");
        }
    }
}
