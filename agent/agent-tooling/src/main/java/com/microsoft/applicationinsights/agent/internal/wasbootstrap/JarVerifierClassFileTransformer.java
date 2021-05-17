package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static net.bytebuddy.jar.asm.Opcodes.*;

public class JarVerifierClassFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!className.equals("java/util/jar/JarVerifier")) {
            return null;
        }
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new JarFileClassVisitor(cw);
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            // logging hasn't been initialized yet
            t.printStackTrace();
            return null;
        }
    }

    private static class JarFileClassVisitor extends ClassVisitor {

        private final ClassWriter cw;

        private JarFileClassVisitor(ClassWriter cw) {
            super(ASM7, cw);
            this.cw = cw;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("update")) {
                return new JarFileMethodVisitor(mv);
            } else {
                return mv;
            }
        }
    }

    private static class JarFileMethodVisitor extends MethodVisitor {

        private JarFileMethodVisitor(MethodVisitor mv) {
            super(ASM7, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil",
                    "onEnter", "()V", false);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == RETURN || opcode == IRETURN || opcode == FRETURN || opcode == ARETURN
                    || opcode == LRETURN || opcode == DRETURN || opcode == ATHROW) {
                mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil",
                        "onExit", "()V", false);
            }
            super.visitInsn(opcode);
        }
    }
}
