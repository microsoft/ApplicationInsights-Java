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

package com.microsoft.applicationinsights.internal.reflect;

import org.junit.Test;

import static org.junit.Assert.*;

public final class ClassDataVerifierImplTests {
    private final static String PUBLIC_EXISTING_METHOD = "endsWith";
    private final static String PUBLIC_NOT_EXISTING_METHOD = "notexistingmethod";
    private final static String EXISTING_CLASS = "java.lang.String";
    private final static String NOT_EXISTING_CLASS = "java.lang.StringStringString";

    @Test
    public void testMethodExistingPublicMethod() {
        boolean found = new ClassDataVerifierImpl().isMethodExists(String.class, PUBLIC_EXISTING_METHOD, String.class);
        assertTrue("Method not found", found);
    }

    @Test
    public void testMethodNotExistingPublicMethod() {
        boolean found = new ClassDataVerifierImpl().isMethodExists(String.class, PUBLIC_NOT_EXISTING_METHOD);
        assertFalse("Method found", found);
    }

    @Test
    public void testClassExists() {
        boolean found = new ClassDataVerifierImpl().isClassExists(EXISTING_CLASS);
        assertTrue("Class not found", found);
    }

    @Test
    public void testClassDoesNotExist() {
        boolean found = new ClassDataVerifierImpl().isClassExists(NOT_EXISTING_CLASS);
        assertFalse("Class found", found);
    }
}
