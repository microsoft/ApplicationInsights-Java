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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Helper class for reading data from a properties file found on the class path.
 */
public class PropertyHelper
{
    /**
     * Reads the properties from a properties file.
     * @param name of the properties file.
     * @return A {@link Properties} object containing the properties read from the provided file.
     * @throws IOException in case
     */
    public static Properties getProperties(String name) throws IOException
    {
        Properties props = new Properties();
        ClassLoader classLoader = PropertyHelper.class.getClassLoader();

        // Look in the class loader's default location.
        InputStream inputStream = classLoader.getResourceAsStream(name);
        if (inputStream != null)
        {
            try {
                props.load(inputStream);
            } finally {
                inputStream.close();
            }
        }

        return props;
    }
}

