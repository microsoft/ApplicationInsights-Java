package com.microsoft.applicationinsights.framework;

import java.io.*;
import java.util.Properties;

/**
 * Created by amnonsh on 5/28/2015.
 */
public class TestEnvironment {
    private Properties envProps;

    String configurationEnvironmentVariable = "AI_JAVASDK_TEST_ENVIRONMENT_SETTINGS_PATH";

    public static final String KEY_APPLICATION_SERVER                    = "env.applicationServer";
    public static final String KEY_APPLICATION_SERVER_PORT               = "env.applicationServer.port";
    public static final String KEY_APPLICATION_NAME                      = "env.application.name";
    public static final String KEY_APPLICATION_STORAGE_CONNECTION_STRING = "env.application.storage.connectionString";
    public static final String KEY_APPLICATION_STORAGE_EXPORT_QUEUE_NAME = "env.application.storage.exportQueueName";

    public TestEnvironment() throws IOException {
        String envPropsFilePath = System.getenv(configurationEnvironmentVariable);
        if (envPropsFilePath == null) {
            throw new RuntimeException("Environment variable " + configurationEnvironmentVariable + " not defined");
        }

        envProps = loadPropertiesFromFile(envPropsFilePath);
    }

    public String getApplicationServer() {
        return envProps.getProperty(KEY_APPLICATION_SERVER);
    }

    public Integer getApplicationServerPort() {
        return Integer.parseInt(envProps.getProperty(KEY_APPLICATION_SERVER_PORT));
    }

    public String getApplicationName() {
        return envProps.getProperty(KEY_APPLICATION_NAME);
    }

    public String getApplicationStorageConnectionString() {
        return envProps.getProperty(KEY_APPLICATION_STORAGE_CONNECTION_STRING);
    }

    public String getApplicationStorageExportQueueName() {
        return envProps.getProperty(KEY_APPLICATION_STORAGE_EXPORT_QUEUE_NAME);
    }

    public static Properties loadPropertiesFromFile(String filePath) throws IOException
    {
        File configFile = new File(filePath);
        if (!configFile.exists()) {
            throw new FileNotFoundException("Configuration file \"" + filePath + "\" could not be found");
        }

        System.out.println("Found configuration file at: " + filePath);

        InputStream inputStream = new FileInputStream(filePath);

        return readFromStream(inputStream);
    }

    private static Properties readFromStream(InputStream inputStream) throws IOException {
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
