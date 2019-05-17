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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by gupele on 7/27/2015.
 */
public final class OkHttpClassVisitor extends DefaultClassVisitor {
    private final static String REQUEST_CLASS_NAME = "Lcom/squareup/okhttp/Request;";

    private String requestFieldName;

    public OkHttpClassVisitor(ClassInstrumentationData instrumentationData, ClassWriter classWriter) {
        super(instrumentationData, classWriter);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (REQUEST_CLASS_NAME.equals(desc)) {
            requestFieldName = name;
        }

        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    protected MethodVisitor getMethodVisitor(int access, String name, String desc, MethodVisitor originalMV) {
        MethodVisitor mv = instrumentationData.getMethodVisitor(access, name, desc, originalMV, new OkHttpClassToMethodTransformationData(requestFieldName));
        return mv;
    }
}
