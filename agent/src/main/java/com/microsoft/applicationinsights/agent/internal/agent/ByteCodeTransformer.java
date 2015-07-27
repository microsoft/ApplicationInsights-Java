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

package com.microsoft.applicationinsights.agent.internal.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Created by gupele on 7/27/2015.
 */
final class ByteCodeTransformer {
    private final ClassInstrumentationData classInstrumentationData;

    ByteCodeTransformer(ClassInstrumentationData classInstrumentationData) {
        this.classInstrumentationData = classInstrumentationData;
    }

    /**
     * The method will create the the instances that are responsible for transforming the class' code.
     * @param originalBuffer The original buffer of the class
     * @return A new buffer containing the class with the changes or the original one if no change was done.
     */
    byte[] transform(byte[] originalBuffer) {
        if (classInstrumentationData == null) {
            return originalBuffer;
        }

        System.out.println("class::" + classInstrumentationData.getClassName());
        ClassReader cr = new ClassReader(originalBuffer);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        DefaultClassVisitor dcv = classInstrumentationData.getDefaultClassInstrumentor(cw);
        cr.accept(dcv, ClassReader.EXPAND_FRAMES);
        byte[] newBuffer = cw.toByteArray();
        return newBuffer;
    }
}
