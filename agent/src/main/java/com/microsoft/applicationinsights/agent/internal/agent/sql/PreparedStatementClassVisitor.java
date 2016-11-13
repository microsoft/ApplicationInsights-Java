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

import java.util.HashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.microsoft.applicationinsights.agent.internal.agent.ByteCodeUtils;
import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;

/**
 * Created by gupele on 8/3/2015.
 */
public final class PreparedStatementClassVisitor extends ClassVisitor {

    private final HashMap<String, String> nameAndSignature = new HashMap<String, String>();

    protected final ClassInstrumentationData instrumentationData;
    private final PreparedStatementMetaData metaData;
    private boolean shouldAdd = true;

    public PreparedStatementClassVisitor(ClassInstrumentationData instrumentationData, ClassWriter classWriter, PreparedStatementMetaData metaData) {
        super(Opcodes.ASM5, classWriter);

        this.instrumentationData = instrumentationData;
        this.metaData = metaData;

        nameAndSignature.put("setBoolean", "(IZ)V");
        nameAndSignature.put("setInt", "(II)V");
        nameAndSignature.put("setShort", "(IS)V");
        nameAndSignature.put("setDouble", "(ID)V");
        nameAndSignature.put("setFloat", "(IF)V");
        nameAndSignature.put("setString", "(ILjava/lang/String;)V");

        nameAndSignature.put("setBigDecimal", "(ILjava/math/BigDecimal;)V");

        nameAndSignature.put("setObject", "(ILjava/lang/Object;)V");
        nameAndSignature.put("setTimestamp", "(ILjava/sql/Timestamp;)V");
        nameAndSignature.put("setTimestamp", "(ILjava/sql/Timestamp;Ljava/util/Calendar)V");
        nameAndSignature.put("setTime", "(ILjava/sql/Time;)V");
        nameAndSignature.put("setDate", "(ILjava/sql/Date;)V");

        nameAndSignature.put("setBlob", "(ILjava/sql/Blob;)V");

        nameAndSignature.put("setNull", "(II)V");
/*
		*/
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        if (fv != null && shouldAdd) {
            shouldAdd = false;

            FieldVisitor fv1 = super.visitField(Opcodes.ACC_PROTECTED, SqlConstants.AI_SDK_SQL_STRING, "Ljava/lang/String;", null, null);
            if (fv1 != null) {
                fv1.visitEnd();
            }
            fv1 = super.visitField(Opcodes.ACC_PROTECTED, SqlConstants.AI_SDK_ARGS_ARRAY, "[Ljava/lang/Object;", null, null);
            if (fv1 != null) {
                fv1.visitEnd();
            }
            fv1 = super.visitField(Opcodes.ACC_PROTECTED, SqlConstants.AI_SDK_BATCH_COUNTER, "I", null, null);
            if (fv1 != null) {
                fv1.visitEnd();
            }
        }
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor originalMV = super.visitMethod(access, name, desc, signature, exceptions);

        if (originalMV == null) {
            return originalMV;
        }

        if (ByteCodeUtils.isConstructor(name) && metaData.ctorSignatures.contains(desc)) {
            return new PreparedStatementCtorMethodVisitor(access, desc, instrumentationData.getClassName(), name, originalMV, metaData);
        }

        String foundDesc = nameAndSignature.get(name);
        if (desc != null && desc.equals(foundDesc)) {
            return new PreparedStatementSetMethod(access, desc, instrumentationData.getClassName(), name, originalMV, metaData);
        }

        return instrumentationData.getMethodVisitor(access, name, desc, originalMV, metaData);
    }
}
