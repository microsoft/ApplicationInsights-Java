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

import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;

import org.junit.Test;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;

public final class EnterExitClassVisitorTest {
//    @Test
//    public void testVisitMethodForInterfaceClass() {
//        MethodInstrumentorsFactory mockFactory = Mockito.mock(MethodInstrumentorsFactory.class);
//        ClassInstrumentationData classInstrumentationData = new ClassInstrumentationData("java/lang/Runnable", InstrumentedClassType.OTHER);
//        classInstrumentationData.addMethod("run", null, true, true);
//        DefaultClassVisitor tested = new DefaultClassVisitor(mockFactory, classInstrumentationData, new ClassWriter(10));
//        tested.visit(Opcodes.ASM5, Opcodes.ACC_INTERFACE, "java/lang/Runnable", "()V", null, null);
//        tested.visitMethod(Opcodes.ACC_INTERFACE, "run", "()V", null, new String[]{});
//
//        Mockito.verify(mockFactory, Mockito.never()).getMethodVisitor(any(MethodInstrumentationDecision.class), anyInt(), anyString(), anyString(), anyString(), any(MethodVisitor.class));
//    }
//
//    @Test
//    public void testVisitCtor1() {
//        MethodInstrumentorsFactory mockFactory = Mockito.mock(MethodInstrumentorsFactory.class);
//        ClassInstrumentationData classInstrumentationData = new ClassInstrumentationData("java/lang/Runnable", InstrumentedClassType.OTHER);
//        DefaultClassVisitor tested = new DefaultClassVisitor(mockFactory, classInstrumentationData, new ClassWriter(10));
//        tested.visit(Opcodes.ASM5, ~Opcodes.ACC_INTERFACE, "java/lang/String", "()V", null, null);
//        tested.visitMethod(~Opcodes.ACC_INTERFACE, "<init>", "()V", null, new String[]{});
//
//        Mockito.verify(mockFactory, Mockito.never()).getMethodVisitor(any(MethodInstrumentationDecision.class), anyInt(), anyString(), anyString(), anyString(), any(MethodVisitor.class));
//    }
//
//    @Test
//    public void testVisitCtor2() {
//        MethodInstrumentorsFactory mockFactory = Mockito.mock(MethodInstrumentorsFactory.class);
//        ClassInstrumentationData classInstrumentationData = new ClassInstrumentationData("java/lang/String", InstrumentedClassType.OTHER);
//        DefaultClassVisitor tested = new DefaultClassVisitor(mockFactory, classInstrumentationData, new ClassWriter(10));
//        tested.visit(Opcodes.ASM5, ~Opcodes.ACC_INTERFACE, "java/lang/String", "()V", null, null);
//        tested.visitMethod(~Opcodes.ACC_INTERFACE, "<clinit>", "()V", null, new String[]{});
//
//        Mockito.verify(mockFactory, Mockito.never()).getMethodVisitor(any(MethodInstrumentationDecision.class), anyInt(), anyString(), anyString(), anyString(), any(MethodVisitor.class));
//    }
//
//    @Test
//    public void testVisitQualifiedMethod() {
//        MethodInstrumentorsFactory mockFactory = Mockito.mock(MethodInstrumentorsFactory.class);
//        ClassInstrumentationData classInstrumentationData = new ClassInstrumentationData("java/lang/String", InstrumentedClassType.OTHER);
//        classInstrumentationData.addMethod("indexOf", null, true, true);
//        DefaultClassVisitor tested = new DefaultClassVisitor(mockFactory, classInstrumentationData, new ClassWriter(10));
//        tested.visit(Opcodes.ASM5, ~Opcodes.ACC_INTERFACE, "java/lang/String", "()V", null, null);
//        tested.visitMethod(~(Opcodes.ACC_INTERFACE | Opcodes.ACC_PRIVATE), "indexOf", "(Ljava/lang/String;)V", null, new String[]{});
//
//        Mockito.verify(mockFactory, Mockito.times(1)).getMethodVisitor(any(MethodInstrumentationDecision.class), anyInt(), anyString(), anyString(), anyString(), any(MethodVisitor.class));
//    }
}
