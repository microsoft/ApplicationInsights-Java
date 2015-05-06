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

package com.microsoft.applicationinsights.collectd;

import java.util.HashMap;
import java.util.List;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.collectd.internal.ApplicationInsightsWriterLogger;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.collectd.api.*;

/**
 * Created by yonisha on 4/26/2015.
 */
public class ApplicationInsightsWriter implements CollectdConfigInterface,
        CollectdInitInterface,
        CollectdWriteInterface,
        CollectdShutdownInterface {

    // region Consts

    private static final String PLUGIN_NAME = "ApplicationInsightsWriter";
    private static final String INSTRUMENTATION_KEY_CONFIGURATION_KEY = "InstrumentationKey";
    private static final String SDK_LOGGER_CONFIGURATION_KEY = "SDKLogger";

    // endregion Consts

    // region Members

    private TelemetryConfiguration telemetryConfiguration;
    private TelemetryClient telemetryClient;
    private ApplicationInsightsWriterLogger logger = null;

    // endregion Members

    // region Ctors

    public ApplicationInsightsWriter() {
        Collectd.registerConfig(PLUGIN_NAME, this);
        Collectd.registerInit(PLUGIN_NAME, this);
        Collectd.registerWrite(PLUGIN_NAME, this);
        Collectd.registerShutdown(PLUGIN_NAME, this);

        this.logger = new ApplicationInsightsWriterLogger();
        this.telemetryConfiguration = TelemetryConfiguration.getActive();
    }

    protected ApplicationInsightsWriter(TelemetryClient telemetryClient, ApplicationInsightsWriterLogger logger) {
        this.telemetryClient = telemetryClient;
        this.telemetryConfiguration = TelemetryConfiguration.getActive();
        this.logger = logger;
    }

    // endregion Ctors

    // region Public

    /**
     * Reads the configuration provided for the writer plugin.
     * @param configuration The configuration.
     * @return Zero on success, false otherwise.
     */
    public int config(OConfigItem configuration) {
        logger.logInfo("Loading configuration.");
        logger.logDebug("Configuration found: " + configuration);

        List<OConfigItem> children = configuration.getChildren();
        if (children.size() < 1) {
            logger.logError("Configuration error: application instrumentation key must be set.");

            return 1;
        }

        for (int i = 0; i < children.size(); i++) {
            OConfigItem child = children.get(i);
            String key = child.getKey();

            if (key.equalsIgnoreCase(INSTRUMENTATION_KEY_CONFIGURATION_KEY)) {
                List<OConfigValue> values = child.getValues();
                if (values.size() != 1) {
                    logger.logError(key + " configuration option needs exactly 1 argument.");
                    return 2;
                }

                String instrumentationKey = values.get(0).toString();
                if (LocalStringsUtils.isNullOrEmpty(instrumentationKey)) {
                    logger.logError (INSTRUMENTATION_KEY_CONFIGURATION_KEY + "' configuration option is mandatory, plugin will be disabled");

                    return 1;
                }

                this.telemetryConfiguration.setInstrumentationKey(instrumentationKey);
            } else if (key.equalsIgnoreCase(SDK_LOGGER_CONFIGURATION_KEY)) {
                List<OConfigValue> values = child.getValues();
                if (values.size() != 1) {
                    logger.logError(key + " configuration option needs exactly 1 argument [true/false].");
                    return 2;
                }

                boolean enableSDKLogger = Boolean.parseBoolean(values.get(0).toString());
                if (enableSDKLogger) {
                    InternalLogger.INSTANCE.initialize("CONSOLE", new HashMap<String, String>());
                }
            } else {
                logger.logWarning("Unknown configuration option '" + key + "'.");
            }
        }

        logger.logInfo("Configuration loaded successfully.");

        return 0;
    }

    /**
     * Initializes the writer plugin.
     * @return Zero on success, non-zero otherwise.
     */
    public int init() {
        logger.logInfo("Initializing...");

        try {
            this.telemetryClient = new TelemetryClient(this.telemetryConfiguration);

            logger.logInfo("Initialization completed.");
        } catch (Throwable e ) {
            logger.logError("Initialization failed, plugin will be disabled:\n" + e.toString());

            return 1;
        }

        return 0;
    }

    /**
     * Shutting down the writer plugin.
     * @return Zero on success, non-zero otherwise.
     */
    public int shutdown() {
        return 0;
    }

    /**
     * Writes the given values to Application Insights.
     * @param valueList The values to be sent.
     * @return Zero on success, non-zero otherwise.
     */
    public int write(ValueList valueList) {
        // We're avoiding writing traces in this method to not spam the logs, as this method is being called very frequently.
        logger.logDebug("Writing values (" + valueList.getValues().size() + "):\n" + valueList.toString());

        for (int i = 0; i < valueList.getValues().size(); i++) {
            MetricTelemetry metricTelemetry = createMetricTelemetry(valueList, i);
            this.telemetryClient.trackMetric(metricTelemetry);
        }

        return 0;
    }

    // endregion Public

    // region Private

    private static MetricTelemetry createMetricTelemetry(ValueList valueList, int index) {
        DataSource dataSource = getDataSource(valueList.getDataSet(), index);

        MetricTelemetry telemetry = new MetricTelemetry();

        if (dataSource != null) {
            telemetry.setMin(dataSource.getMin());
            telemetry.setMax(dataSource.getMax());
        }

        String telemetryName = generateMetricName(valueList, dataSource);
        telemetry.setName(telemetryName);

        Number value = valueList.getValues().get(index);
        if (value != null) {
            telemetry.setValue(value.doubleValue());
        }

        return telemetry;
    }

    protected static String generateMetricName(ValueList valueList, DataSource dataSource) {
        String metricName = valueList.getSource();

        if (dataSource != null) {
            metricName = metricName.concat("/" + dataSource.getName());
        }

        return metricName;
    }

    private static DataSource getDataSource(DataSet dataSet, int index) {
        DataSource dataSource = null;
        if (dataSet != null) {
            dataSource = dataSet.getDataSources().get(index);
        }

        return dataSource;
    }

    // endregion Private
}
