package com.microsoft.applicationinsights.web.spring;

import com.microsoft.applicationinsights.test.utils.PropertiesUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by amnonsh on 5/28/2015.
 */
public class TestEnvironment {
    private Properties envProps;

    String configurationEnvironmentVariable = "AI_JAVASDK_TEST_ENVIRONMENT_SETTINGS_PATH";

    private final String KEY_APPLICATION_SERVER                    = "env.applicationServer";
    private final String KEY_APPLICATION_SERVER_PORT               = "env.applicationServer.port";
    private final String KEY_APPLICATION_NAME                      = "env.application.name";
    private final String KEY_APPLICATION_STORAGE_CONNECTION_STRING = "env.application.storage.connectionString";
    private final String KEY_APPLICATION_STORAGE_EXPORT_QUEUE_NAME = "env.application.storage.exportQueueName";

    public TestEnvironment() throws IOException {
        String envPropsFilePath = System.getenv(configurationEnvironmentVariable);
        if (envPropsFilePath == null) {
            throw new RuntimeException("Environment variable " + configurationEnvironmentVariable + " not defined");
        }

        envProps = PropertiesUtils.loadPropertiesFromFile(envPropsFilePath);
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
}
