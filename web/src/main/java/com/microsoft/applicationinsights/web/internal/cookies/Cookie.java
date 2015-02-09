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

package com.microsoft.applicationinsights.web.internal.cookies;

import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Created by yonisha on 2/9/2015.
 *
 * Used as a base class for AI cookies.
 */
public class Cookie {
    protected static final String RAW_COOKIE_DELIMITER = "|";
    protected static final String RAW_COOKIE_SPLIT_DELIMITER = "\\" + RAW_COOKIE_DELIMITER;

    /**
     * Validates if the given string is of type UUID.
     * @param possibleUUID The possible UUID.
     * @throws Exception Thrown if the string is not UUID.
     */
    protected static void validateUUID(String possibleUUID) throws Exception {
        if (!Sanitizer.isUUID(possibleUUID)) {
            String errorMessage  = String.format("Given ID '%s' is not of type UUID.", possibleUUID);

            throw new Exception(errorMessage);
        }
    }
}
