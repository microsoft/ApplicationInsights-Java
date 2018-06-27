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

package com.microsoft.applicationinsights.agent.internal.agent.redis;

import com.microsoft.applicationinsights.agent.internal.agent.ClassToMethodTransformationData;
import com.microsoft.applicationinsights.agent.internal.agent.DefaultMethodVisitor;
import org.objectweb.asm.MethodVisitor;

/** The class is responsible for instrumenting Jedis client methods. */
final class JedisMethodVisitorV2 extends DefaultMethodVisitor {
  private static final String ON_ENTER_METHOD_NAME = "jedisMethodStarted";
  private static final String ON_ENTER_METHOD_SIGNATURE = "(Ljava/lang/String;)V";

  public JedisMethodVisitorV2(
      int access,
      String desc,
      String owner,
      String methodName,
      MethodVisitor methodVisitor,
      ClassToMethodTransformationData additionalData) {
    super(false, true, 0, access, desc, owner, methodName, methodVisitor, additionalData);
  }

  @Override
  protected String getOnEnterMethodName() {
    return ON_ENTER_METHOD_NAME;
  }

  @Override
  protected String getOnEnterMethodSignature() {
    return ON_ENTER_METHOD_SIGNATURE;
  }
}
