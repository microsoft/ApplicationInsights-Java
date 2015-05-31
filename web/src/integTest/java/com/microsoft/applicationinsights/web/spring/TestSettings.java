package com.microsoft.applicationinsights.web.spring;

import com.microsoft.applicationinsights.test.utils.PropertiesUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by amnonsh on 5/28/2015.
 */
public class TestSettings {
    private Properties testProps;

    private final String SETTINGS_RESOURCE_NAME  = "testSettings.properties";

    private final String KEY_MAX_WAIT_TIME       = "maxWaitTime";
    private final String KEY_POLLING_INTERVAL    = "keyPollingInterval";
    private final String KEY_MESSAGE_BATCH_SIZE  = "keyMessageBatchSize";

    public TestSettings() throws IOException {
        testProps = PropertiesUtils.loadPropertiesFromResource(SETTINGS_RESOURCE_NAME);
    }

    public Integer getMaxWaitTime() {
        return Integer.parseInt(testProps.getProperty(KEY_MAX_WAIT_TIME));
    }

    public Integer getPollingInterval() {
        return Integer.parseInt(testProps.getProperty(KEY_POLLING_INTERVAL));
    }

    public Integer getMessageBatchSize() {
        return Integer.parseInt(testProps.getProperty(KEY_MESSAGE_BATCH_SIZE));
    }
}
