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
        super(false, false, 0, access, desc, owner, methodName, methodVisitor, additionalData);
        this.metaData = (PreparedStatementMetaData)additionalData;
        this.methodName = methodName;
    }
        int localIndex;
        int tmpArrayIndex;

    @Override
    protected void onMethodEnter() {
		if (!"setInt".equals(methodName)) {
			return;
		}
		if (!"setShort".equals(methodName)) {
			return;
		}

		if (!"setBoolean".equals(methodName)) {
			return;
		}

        localIndex = this.newLocal(Type.INT_TYPE);
        tmpArrayIndex = this.newLocal(Type.getType(Object.class));

		System.out.println("index: " + methodName + " " + localIndex + " " + tmpArrayIndex);
		
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
		Label l0 = new Label();
		mv.visitJumpInsn(IFNONNULL, l0);
		mv.visitVarInsn(ILOAD, 1);
		mv.visitInsn(ICONST_1);
		mv.visitInsn(IADD);
		mv.visitVarInsn(ISTORE, localIndex);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ILOAD, 3);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		mv.visitFieldInsn(PUTFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
		Label l1 = new Label();
		mv.visitJumpInsn(GOTO, l1);
		mv.visitLabel(l0);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
		mv.visitInsn(ARRAYLENGTH);
		mv.visitVarInsn(ISTORE, localIndex);
		mv.visitVarInsn(ILOAD, localIndex);
		mv.visitVarInsn(ILOAD, 1);
		mv.visitJumpInsn(IF_ICMPGE, l1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
		mv.visitVarInsn(ASTORE, tmpArrayIndex);
		mv.visitVarInsn(ALOAD, 0);
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
		mv.visitLabel(l1);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, owner, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;");
		mv.visitVarInsn(ILOAD, 1);

		addArgument();
		mv.visitInsn(AASTORE);
    }

    @Override
    protected void byteCodeForMethodExit(int opcode) {
    }

    private void addArgument() {
			System.out.println("add argument Added b+" + methodName);

        if ("setBoolean".equals(methodName)) {
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
			System.out.println("Added b");
        } else if ("setInt".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			System.out.println("Added i");
        } else if ("setLong".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
			System.out.println("Added l");
        } else if ("setShort".equals(methodName)) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
        } else if ("setDouble".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if ("setFloat".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        } else if ("setString".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
        } else if ("setBigDecimal".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
        } else if ("setTimestamp".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
        } else if ("setTime".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
        } else if ("setDate".equals(methodName)) {
            mv.visitVarInsn(LLOAD, 2);
        } else if ("setBlob".equals(methodName)) {
            mv.visitLdcInsn("BLOB");
        } else if ("setNull".equals(methodName)) {
            mv.visitLdcInsn("null");
        } else {
            mv.visitLdcInsn("UNKNOWN");
        }
    }
}
