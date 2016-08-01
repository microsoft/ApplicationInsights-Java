package com.microsoft.applicationinsights.agent.internal.agent.exceptions;

import com.microsoft.applicationinsights.agent.internal.agent.AdvancedAdviceAdapter;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
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
