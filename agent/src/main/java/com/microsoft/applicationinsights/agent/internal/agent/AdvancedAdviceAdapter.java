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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * A base class that lets its derived classes to enjoy utilities
 * like creating temporary variables, managing methods points, method names etc.
 *
 * Created by gupele on 5/11/2015.
 */
abstract class AdvancedAdviceAdapter extends AdviceAdapter {

    protected enum ExitStatus {
        EXIT_WITH_EXCEPTION,
        EXIT_WITH_RETURN_VALUE,
        EXIT_VOID,
        EXIT_UNKNOWN
    }

    protected static class TempVar {
        public final int tempVarIndex;

        public TempVar(int tempVarIndex) {
            this.tempVarIndex = tempVarIndex;
        }
    }

    protected static class TempArrayVar {
        public final int tempVarIndex;

        public TempArrayVar(int tempVarIndex) {
            this.tempVarIndex = tempVarIndex;
        }
    }

    private String methodName;
    private String desc;

    private Label startTryFinallyBlock = new Label();
    private Label endTryFinallyBlock = new Label();

    protected final boolean reportExecutionTime;

    @Override
    public void visitCode() {
        mark(startTryFinallyBlock);
        super.visitCode();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        visitTryCatchBlock(startTryFinallyBlock, endTryFinallyBlock, endTryFinallyBlock, null);
        mark(endTryFinallyBlock);

        byteCodeForMethodExit(Opcodes.ATHROW);

        mv.visitInsn(Opcodes.ATHROW);
        mv.visitMaxs(maxStack, maxLocals);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (!reportExecutionTime) {
            return;
        }

        if (opcode != Opcodes.ATHROW) {
            byteCodeForMethodExit(opcode);
        }
    }

//    @Override
//    public void visitMethodInsn(int opcode,String owner,String name,String desc, boolean isMethodOwnerAnInterface) {
//        super.visitMethodInsn(opcode, owner, name, desc, isMethodOwnerAnInterface);
//    }
//
    protected AdvancedAdviceAdapter(boolean reportExecutionTime,
                                    int api,
                                    MethodVisitor methodVisitor,
                                    int access,
                                    String owner,
                                    String methodName,
                                    String desc) {
        super(api, methodVisitor, access, methodName, desc);
        this.reportExecutionTime = reportExecutionTime;
        this.desc = desc;
        this.methodName = owner + "." + methodName;
    }

    protected void duplicateTop(Type type) {
        if (type.getSize() == 2) {
            dup2();
        } else {
            dup();
        }
    }

    /**
     * This method calls a Java enum method with its expected argumentes
     * @param clazz The enum class to activate
     * @param methodName The method of the class to activate
     * @param methodSignature The method signature
     * @param args The arguments to pass to the method
     */
    protected void activateEnumMethod(Class<?> clazz, String methodName, String methodSignature, Object... args) {
        String internalName = Type.getInternalName(clazz);
        super.visitFieldInsn(Opcodes.GETSTATIC, internalName, "INSTANCE", "L" + internalName + ";");
        for (Object arg : args) {
            if (arg instanceof TempVar) {
                loadLocal(((TempVar) arg).tempVarIndex);
            } else if (arg instanceof TempArrayVar) {
                super.visitVarInsn(Opcodes.ALOAD, ((TempArrayVar) arg).tempVarIndex);
            } else {
                super.visitLdcInsn(arg);
            }
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, methodName, methodSignature, false);
    }

    protected TempVar duplicateTopStackToTempVariable(Type typeOfTopElementInStack) {
        duplicateTop(typeOfTopElementInStack);
        int tempVarIndex = newLocal(typeOfTopElementInStack);
        storeLocal(tempVarIndex, typeOfTopElementInStack);

        return new TempVar(tempVarIndex);
    }

    protected String getMethodName() {
        return methodName;
    }

    protected Type getReturnType() {
        return Type.getReturnType(desc);
    }

    protected ExitStatus translateExitCode(int opcode) {
        switch (opcode) {
            case Opcodes.ATHROW:
                return ExitStatus.EXIT_WITH_EXCEPTION;

            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
                return ExitStatus.EXIT_WITH_RETURN_VALUE;

            case Opcodes.RETURN:
                return ExitStatus.EXIT_VOID;

            default:
                return ExitStatus.EXIT_UNKNOWN;
        }
    }

    protected abstract void byteCodeForMethodExit(int opcode);
}
