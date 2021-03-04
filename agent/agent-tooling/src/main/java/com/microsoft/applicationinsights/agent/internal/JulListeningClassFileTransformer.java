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
package com.microsoft.applicationinsights.agent.internal;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.concurrent.CountDownLatch;

class JulListeningClassFileTransformer implements ClassFileTransformer {

    // using constant here so that it will NOT get shaded
    // IMPORTANT FOR THIS NOT TO BE FINAL (or private)
    // OTHERWISE COMPILER COULD THEORETICALLY INLINE IT BELOW AND APPLY .substring(1)
    // and then it WOULD be shaded
    static String UNSHADED_PREFIX = "!java/util/logging/Logger";

    private final String unshadedClassName = UNSHADED_PREFIX.substring(1);

    private final CountDownLatch latch;

    JulListeningClassFileTransformer(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public byte /*@Nullable*/[] transform(@Nullable ClassLoader loader, @Nullable String className,
                                          @Nullable Class<?> classBeingRedefined,
                                          @Nullable ProtectionDomain protectionDomain,
                                          byte[] classfileBuffer) {

        if (unshadedClassName.equals(className)) {
            latch.countDown();
        }
        return null;
    }
}
