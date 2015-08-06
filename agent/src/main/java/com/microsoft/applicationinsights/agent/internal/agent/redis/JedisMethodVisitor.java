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

package com.microsoft.applicationinsights.agent.internal.agent.redis;

import com.microsoft.applicationinsights.agent.internal.agent.ByteCodeUtils;
import com.microsoft.applicationinsights.agent.internal.agent.ClassToMethodTransformationData;
import com.microsoft.applicationinsights.agent.internal.agent.DefaultMethodVisitor;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by gupele on 8/6/2015.
 */
final class JedisMethodVisitor extends DefaultMethodVisitor {
    private final static String FINISH_DETECT_METHOD_NAME = "methodFinished";
    private final static String FINISH_METHOD_DEFAULT_SIGNATURE = "(Ljava/lang/String;J[Ljava/lang/Object;Ljava/lang/Throwable;)V";

    private Type[] argumentTypes;

    private boolean isStatic;
    private int firstEmptyIndexForLocalVariable;

    public JedisMethodVisitor(int access,
                              String desc,
                              String owner,
                              String methodName,
                              MethodVisitor methodVisitor,
                              ClassToMethodTransformationData additionalData) {
        super(false, true, access, desc, owner, methodName, methodVisitor, additionalData);

        argumentTypes = Type.getArgumentTypes(desc);
        firstEmptyIndexForLocalVariable = argumentTypes.length;
        for (Type tp : argumentTypes) {
            if (tp.equals(Type.LONG_TYPE) || tp.equals(Type.DOUBLE_TYPE)) {
                ++firstEmptyIndexForLocalVariable;
            }
        }

        isStatic = ByteCodeUtils.isStatic(access);
        if (!isStatic) {
            ++firstEmptyIndexForLocalVariable;
        }
    }

    private int localStart;
    private int localFinish;
    private int localDelta;

    @Override
    public void onMethodEnter() {
        localStart = this.newLocal(Type.getType(Long.class));
        localFinish = this.newLocal(Type.getType(Long.class));
        localDelta = this.newLocal(Type.getType(Long.class));

        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        mv.visitVarInsn(LSTORE, localStart);
    }

    @Override
    protected void byteCodeForMethodExit(int opcode) {

        switch (translateExitCode(opcode)) {
            case EXIT_WITH_EXCEPTION:
                TempVar throwable = duplicateTopStackToTempVariable(Type.getType(Throwable.class));
                finished(throwable);
                break;

            case EXIT_WITH_RETURN_VALUE:
            case EXIT_VOID:
                finished(null);
                break;

            default:
                break;
        }
    }

    private void finished(TempVar throwable) {

        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        mv.visitVarInsn(LSTORE, localFinish);
        mv.visitVarInsn(LLOAD, localFinish);
        mv.visitVarInsn(LLOAD, localStart);
        mv.visitInsn(LSUB);
        mv.visitVarInsn(LSTORE, localDelta);
        mv.visitVarInsn(LLOAD, localDelta);

        mv.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);
        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, "getRedisThresholdInNS", "()J", false);

        mv.visitInsn(LCMP);

        Label l0 = new Label();
        mv.visitJumpInsn(IFLE, l0);

        reportFinishWithArgs(throwable);

        Label l1 = new Label();
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l0);

        notifyEndOfMethod(null, throwable);

        mv.visitLabel(l1);
    }

    private void reportFinishWithArgs(TempVar throwable) {

        int boxedArgumentsIndex = 0;
        int argumentIndex = isMethodStatic() ? 0 : 1;

        TempArrayVar arrayOfArgumentsAfterBoxing;
        arrayOfArgumentsAfterBoxing = createArray(numberOfArguments());

        // Fill the array with method parameters after boxing the primitive ones.
        for (Type argumentType : getArgumentTypes()) {
            setBoxedValueIntoArray(arrayOfArgumentsAfterBoxing, boxedArgumentsIndex, argumentType, argumentIndex);

            boxedArgumentsIndex++;
            if (ByteCodeUtils.isLargeType(argumentType)) {
                argumentIndex += 2;
            } else {
                ++argumentIndex;
            }
        }

        notifyEndOfMethod(arrayOfArgumentsAfterBoxing, throwable);
    }

    private void notifyEndOfMethod(TempArrayVar arrayOfArgumentsAfterBoxing, TempVar throwable) {
        super.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);

        mv.visitLdcInsn(getMethodName());
        mv.visitVarInsn(LLOAD, localDelta);
        if (arrayOfArgumentsAfterBoxing != null) {
            mv.visitVarInsn(Opcodes.ALOAD, arrayOfArgumentsAfterBoxing.tempVarIndex);
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        if (throwable != null) {
            loadLocal(throwable.tempVarIndex);
        } else {
            mv.visitInsn(ACONST_NULL);
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, FINISH_DETECT_METHOD_NAME, FINISH_METHOD_DEFAULT_SIGNATURE, false);
    }

    private void reportFinishWithNoArgs(TempVar throwable) {

        super.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);

        mv.visitLdcInsn(getMethodName());
        mv.visitVarInsn(LLOAD, localDelta);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ACONST_NULL);

        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, FINISH_DETECT_METHOD_NAME, FINISH_METHOD_DEFAULT_SIGNATURE, false);
    }

    private void reportFinishWithArgs1(int opcode) {

        int resultIndex = this.newLocal(Type.getType(Integer.class));
        super.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);

        mv.visitLdcInsn(getMethodName());

        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, FINISH_DETECT_METHOD_NAME, FINISH_METHOD_DEFAULT_SIGNATURE, false);

        mv.visitVarInsn(ISTORE, resultIndex);
        mv.visitVarInsn(ILOAD, resultIndex);
        Label ok = new Label();
        mv.visitJumpInsn(IFEQ, ok);

        int boxedArgumentsIndex = 0;
        int argumentIndex = isMethodStatic() ? 0 : 1;

        TempArrayVar arrayOfArgumentsAfterBoxing;
        arrayOfArgumentsAfterBoxing = createArray(numberOfArguments());

        // Fill the array with method parameters after boxing the primitive ones.
        for (Type argumentType : getArgumentTypes()) {
            setBoxedValueIntoArray(arrayOfArgumentsAfterBoxing, boxedArgumentsIndex, argumentType, argumentIndex);

            boxedArgumentsIndex++;
            if (ByteCodeUtils.isLargeType(argumentType)) {
                argumentIndex += 2;
            } else {
                ++argumentIndex;
            }
        }

        super.visitFieldInsn(GETSTATIC, ImplementationsCoordinator.internalName, "INSTANCE", ImplementationsCoordinator.internalNameAsJavaName);

        mv.visitLdcInsn(getMethodName());
        mv.visitVarInsn(Opcodes.ALOAD, arrayOfArgumentsAfterBoxing.tempVarIndex);

        mv.visitMethodInsn(INVOKEVIRTUAL, ImplementationsCoordinator.internalName, FINISH_DETECT_METHOD_NAME, FINISH_METHOD_DEFAULT_SIGNATURE, false);

        mv.visitLabel(ok);

        super.byteCodeForMethodExit(opcode);
    }

    protected TempArrayVar createArray(int length, int extraArgs) {
        firstEmptyIndexForLocalVariable += extraArgs;
        return createArray(length);
    }

    protected TempArrayVar createArray(int length) {
        mv.visitIntInsn(Opcodes.BIPUSH, length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitVarInsn(Opcodes.ASTORE, firstEmptyIndexForLocalVariable);

        return new TempArrayVar(firstEmptyIndexForLocalVariable);
    }

    protected void boxVariable(Type argumentType, int argumentIndex) {
        if (argumentType.equals(Type.BOOLEAN_TYPE)) {
            mv.visitVarInsn(Opcodes.ILOAD, argumentIndex);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        }
        else if (argumentType.equals(Type.BYTE_TYPE)) {
            mv.visitVarInsn(Opcodes.ILOAD, argumentIndex);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
        }
        else if (argumentType.equals(Type.CHAR_TYPE)) {
            mv.visitVarInsn(Opcodes.ILOAD, argumentIndex);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        }
        else if (argumentType.equals(Type.SHORT_TYPE)) {
            mv.visitVarInsn(Opcodes.ILOAD, argumentIndex);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
        }
        else if (argumentType.equals(Type.INT_TYPE)) {
            mv.visitVarInsn(Opcodes.ILOAD, argumentIndex);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        }
        else if (argumentType.equals(Type.LONG_TYPE)) {
            mv.visitVarInsn(Opcodes.LLOAD, argumentIndex);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        }
        else if (argumentType.equals(Type.FLOAT_TYPE)) {
            mv.visitVarInsn(Opcodes.FLOAD, argumentIndex);
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "valueOf", "(F)Ljava/lang/Long;", false);
        }
        else if (argumentType.equals(Type.DOUBLE_TYPE)) {
            mv.visitVarInsn(Opcodes.DLOAD, argumentIndex);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        }
        else {
            mv.visitVarInsn(Opcodes.ALOAD, argumentIndex);
        }
    }

    protected void prepareArrayEntry(int arrayIndex, int entryIndex) {
        mv.visitVarInsn(Opcodes.ALOAD, arrayIndex);
        mv.visitIntInsn(Opcodes.BIPUSH, entryIndex);
    }

    protected void storeTopStackValueIntoArray() {
        mv.visitInsn(Opcodes.AASTORE);
    }

    private void setBoxedValueIntoArray(TempArrayVar arrayOfArgumentsAfterBoxing, int boxedArgumentsIndex, Type argumentType, int argumentIndex) {
        prepareArrayEntry(arrayOfArgumentsAfterBoxing.tempVarIndex, boxedArgumentsIndex);
        boxVariable(argumentType, argumentIndex);
        storeTopStackValueIntoArray();
    }

    protected boolean isMethodStatic() {
        return isStatic;
    }

    protected int numberOfArguments() {
        return argumentTypes.length;
    }

    protected Type[] getArgumentTypes() {
        return argumentTypes;
    }
}
