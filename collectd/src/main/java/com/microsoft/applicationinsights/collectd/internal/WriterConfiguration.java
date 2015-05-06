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

package com.microsoft.applicationinsights.collectd.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import com.microsoft.applicationinsights.collectd.ConfigurationException;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import org.collectd.api.OConfigItem;
import org.collectd.api.OConfigValue;

/**
 * Created by yonisha on 5/5/2015.
 *
 * This class purpose is to parse configuration passed from CollectD framework.
 */
public class WriterConfiguration {

    // region Consts

    public static final String EXCLUDE_CONFIGURATION_KEY = "Exclude";
    public static final String INSTRUMENTATION_KEY_CONFIGURATION_KEY = "InstrumentationKey";
    public static final String SDK_LOGGER_CONFIGURATION_KEY = "SDKLogger";

    // endregion Consts

    // region Members

    private static ApplicationInsightsWriterLogger logger = new ApplicationInsightsWriterLogger();
    private Dictionary<String, PluginExclusion> pluginExclusions = new Hashtable<String, PluginExclusion>();
    private String instrumentationKey;
    private boolean isLoggerEnabled;

    // endregion Members

    /**
     * Gets the instrumentation key.
     * @return The instrumentation key.
     */
    public String getInstrumentationKey() {
        return this.instrumentationKey;
    }

    /**
     * Gets a value indicating whether the logger is enabled or not.
     * @return True if the logger is enabled, false otherwise.
     */
    public boolean getIsLoggerEnabled() {
        return this.isLoggerEnabled;
    }

    /**
     * Gets a dictionary of excluded plugins and data sources.
     * @return Dictionary of excluded plugins and data sources.
     */
    public Dictionary<String, PluginExclusion> getPluginExclusions() {
        return this.pluginExclusions;
    }

    /**
     * Builds @WriterConfiguration from the given CollectD configuration object.
     * @param configuration The CollectD configuration.
     * @return The configuration for Application Insights writer plugin.
     * @throws ConfigurationException Thrown if the configuration is invalid.
     */
    public static WriterConfiguration buildConfiguration(OConfigItem configuration) throws ConfigurationException {
        logger.logDebug("Parsing configuration: " + configuration);

        WriterConfiguration writerConfiguration = new WriterConfiguration();
        List<OConfigItem> children = configuration.getChildren();

        for (OConfigItem child : children) {
            String key = child.getKey();

            if (key.equalsIgnoreCase(INSTRUMENTATION_KEY_CONFIGURATION_KEY)) {
                List<OConfigValue> values = child.getValues();
                if (values.size() != 1) {
                    String errorMessage = key + " configuration option needs exactly 1 argument.";
                    logger.logError(errorMessage);

                    throw new ConfigurationException(errorMessage);
                }

                String instrumentationKey = values.get(0).toString();
                if (LocalStringsUtils.isNullOrEmpty(instrumentationKey)) {

                    String errorMessage = INSTRUMENTATION_KEY_CONFIGURATION_KEY + "' configuration option is mandatory, plugin will be disabled";
                    logger.logError(errorMessage);

                    throw new ConfigurationException(errorMessage);
                }

                writerConfiguration.instrumentationKey = instrumentationKey;
            } else if (key.equalsIgnoreCase(SDK_LOGGER_CONFIGURATION_KEY)) {
                List<OConfigValue> values = child.getValues();
                if (values.size() != 1) {
                    String errorMessage = key + " configuration option needs exactly 1 argument [true/false].";
                    logger.logError(errorMessage);

                    throw new ConfigurationException(errorMessage);
                }

                writerConfiguration.isLoggerEnabled = Boolean.parseBoolean(values.get(0).toString());
            } else if (key.equalsIgnoreCase(EXCLUDE_CONFIGURATION_KEY)) {
                List<OConfigValue> excludes = child.getValues();

                for (OConfigValue value : excludes) {
                    PluginExclusion pluginExclusion = PluginExclusion.buildPluginExclusion(value.toString());

                    if (pluginExclusion != null) {
                        writerConfiguration.pluginExclusions.put(pluginExclusion.getPluginName(), pluginExclusion);
                    }
                }
            } else {
                logger.logWarning("Unknown configuration option '" + key + "'.");
            }
        }

        verifyMandatoryConfigurations(writerConfiguration);

        return writerConfiguration;
    }

    /**
     * Sets the logger for this @WriterConfiguration
     * @param newLogger The logger to use.
     */
    public static void setLogger(ApplicationInsightsWriterLogger newLogger) {
        logger = newLogger;
    }

    private static void verifyMandatoryConfigurations(WriterConfiguration writerConfiguration) throws ConfigurationException {
        if (LocalStringsUtils.isNullOrEmpty(writerConfiguration.getInstrumentationKey())) {
            throw new ConfigurationException("Mandatory configuration %s wasn't found.", INSTRUMENTATION_KEY_CONFIGURATION_KEY);
        }
    }
}
