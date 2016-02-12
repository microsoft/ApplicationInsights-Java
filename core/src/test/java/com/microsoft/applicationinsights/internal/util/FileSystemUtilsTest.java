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

package com.microsoft.applicationinsights.internal.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class FileSystemUtilsTest {

    /*
    NOTE: it doesn't matter that Windows paths are converted to *nix paths and vice-versa.
     */

    @Test
    public void getTempDir_WindowsForUser() {
        final String input = "C:\\Users\\olivida\\AppData\\Local\\Temp";

        final File actual = FileSystemUtils.getTempDir(input);

        final File expected = new File(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getTempDir_MacForUser() {
        final String input = "/var/folders/8b/n6pydwyj1cg9yb7822n277xc0000gs/T/";

        final File actual = FileSystemUtils.getTempDir(input);

        final File expected = new File(input);
        Assert.assertEquals(expected, actual);
    }
}
