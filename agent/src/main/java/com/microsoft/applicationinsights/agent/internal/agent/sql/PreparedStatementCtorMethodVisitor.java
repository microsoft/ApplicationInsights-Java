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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Created by gupele on 8/4/2015. */
final class PreparedStatementCtorMethodVisitor extends DefaultMethodVisitor {
  private final PreparedStatementMetaData metaData;

  public PreparedStatementCtorMethodVisitor(
      int access,
      String desc,
      String owner,
      String methodName,
      MethodVisitor methodVisitor,
      ClassToMethodTransformationData additionalData) {
    super(false, false, 0, access, desc, owner, methodName, methodVisitor, additionalData);
    this.metaData = (PreparedStatementMetaData) additionalData;
  }

  @Override
  public void visitCode() {
    super.visitCode();
    super.visitVarInsn(Opcodes.ALOAD, 0);
    super.visitVarInsn(Opcodes.ALOAD, metaData.sqlStringInCtor);
    super.visitFieldInsn(Opcodes.PUTFIELD, owner, metaData.fieldName, "Ljava/lang/String;");
  }

  @Override
  protected void onMethodExit(int opcode) {}
}
