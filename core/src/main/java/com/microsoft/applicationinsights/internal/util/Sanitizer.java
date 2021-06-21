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

import com.microsoft.applicationinsights.common.Strings;

import java.net.URI;

/**
 * Most of the methods of this class are now obsolete except URL methods which will
 * be moved soon.
 */
public final class Sanitizer {
    public final static int MAX_URL_LENGTH = 2048;

    public static URI sanitizeUri(String urlAsString) {
        if (!Strings.isNullOrEmpty(urlAsString)) {

            if (urlAsString.length() > MAX_URL_LENGTH) {
                urlAsString = urlAsString.substring(0, MAX_URL_LENGTH);
            }

            // In case that the truncated string is invalid
            // URI we will not do nothing and let the Endpoint to drop the property
            URI temp;
            try {
                temp = new URI(urlAsString);
                return temp;
            } catch (Exception e) {
                // Swallow the exception
            }
        }

        return null;
    }

    public static URI safeStringToUri(String url) {
        if (Strings.isNullOrEmpty(url)) {
            return null;
        }

        URI result = null;
        try {
            result = new URI(url);
        } catch (Exception e) {
        }

        return result;
    }
}
