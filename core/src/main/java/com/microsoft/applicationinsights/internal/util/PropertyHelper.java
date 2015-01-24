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

package com.microsoft.applicationinsights.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Helper class for getting data out of the 'applicationinsights.properties' file found on the
 * class path. Typically, it would be found in a JAR or WAR file representing an application
 * or web service deployment.
 */
public class PropertyHelper
{

    private static final String propFileName1 = "applicationinsights.properties";
    private static final String propFileName2 = "buildinfo.properties";

    public static Properties getProperties()
    {
        return s_props;
    }

    static
    {
        s_props = new Properties();

        try
        {
            getPropValues(propFileName1);
            getPropValues(propFileName2);
        }
        catch (IOException e)
        {
            // TODO: log this somewhere.
        }
    }

    private static void getPropValues(String name) throws IOException
    {
        ClassLoader classLoader = PropertyHelper.class.getClassLoader();

        // Look in the class loader's default location.
        InputStream inputStream = classLoader.getResourceAsStream(name);

        if (inputStream != null)
        {
            s_props.load(inputStream);
            inputStream.close();
        }
    }

    private static final Properties s_props;
}

