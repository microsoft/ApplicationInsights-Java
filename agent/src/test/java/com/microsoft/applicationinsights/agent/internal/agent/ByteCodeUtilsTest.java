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

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import org.objectweb.asm.Type;

import static org.junit.Assert.*;

public final class ByteCodeUtilsTest {

    @Test
    public void testIsInterface() throws Exception {
        assertTrue(ByteCodeUtils.isInterface(Opcodes.ACC_INTERFACE));
    }

    @Test
    public void testNotIsInterface() throws Exception {
        assertFalse(ByteCodeUtils.isInterface(~Opcodes.ACC_INTERFACE));
    }

    @Test
    public void testIsAbstract() throws Exception {
        assertTrue(ByteCodeUtils.isAbstract(Opcodes.ACC_ABSTRACT));
    }

    @Test
    public void testNotIsAbstract() throws Exception {
        assertFalse(ByteCodeUtils.isAbstract(~Opcodes.ACC_ABSTRACT));
    }

    @Test
    public void testIsPublic() throws Exception {
        assertTrue(ByteCodeUtils.isPublic(Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testNotIsPublic() throws Exception {
        assertFalse(ByteCodeUtils.isPublic(~Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testIsStatic() throws Exception {
        assertTrue(ByteCodeUtils.isStatic(Opcodes.ACC_STATIC));
    }

    @Test
    public void testNotIsStatic() throws Exception {
        assertFalse(ByteCodeUtils.isStatic(~Opcodes.ACC_STATIC));
    }

    @Test
    public void testIsConstructorInit() throws Exception {
        assertTrue(ByteCodeUtils.isConstructor("<init>"));
    }

    @Test
    public void testIsConstructorCinit() throws Exception {
        assertTrue(ByteCodeUtils.isConstructor("<clinit>"));
    }

    @Test
    public void testIsNonConstructor() throws Exception {
        assertFalse(ByteCodeUtils.isConstructor("bafsd"));
    }

    @Test
    public void testIsLargeType() throws Exception {
        assertTrue(ByteCodeUtils.isLargeType(Type.LONG_TYPE));
        assertTrue(ByteCodeUtils.isLargeType(Type.DOUBLE_TYPE));
    }

    @Test
    public void testIsShortType() throws Exception {
        assertFalse(ByteCodeUtils.isLargeType(Type.INT_TYPE));
        assertFalse(ByteCodeUtils.isLargeType(Type.VOID_TYPE));
    }
}