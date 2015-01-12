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

