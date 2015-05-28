package com.microsoft.applicationinsights.test.utils;

import java.io.*;
import java.util.Properties;

/**
 * Created by amnonsh on 5/28/2015.
 */
public class PropertiesUtils {

    public static Properties loadPropertiesFromResource(String resourceName) throws IOException
    {
        ClassLoader classLoader = PropertiesUtils.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resourceName);
        return loadProperties(inputStream);
    }

    public static Properties loadPropertiesFromFile(String filePath) throws IOException
    {
        File configFile = new File(filePath);
        if (!configFile.exists()) {
            throw new FileNotFoundException("Configuration file \"" + filePath + "\" could not be found");
        }
        System.out.println("Configuration file located in " + filePath);

        InputStream inputStream = new FileInputStream(filePath);
        return loadProperties(inputStream);
    }

    private static Properties loadProperties(InputStream inputStream) throws IOException {
        Properties props = new Properties();

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
