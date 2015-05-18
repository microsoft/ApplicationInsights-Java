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
import java.util.Map;
import javax.naming.ConfigurationException;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.collectd.internal.ApplicationInsightsWriterLogger;
import com.microsoft.applicationinsights.collectd.internal.PluginExclusion;
import com.microsoft.applicationinsights.collectd.internal.WriterConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.collectd.api.CollectdConfigInterface;
import org.collectd.api.CollectdInitInterface;
import org.collectd.api.CollectdShutdownInterface;
import org.collectd.api.CollectdWriteInterface;
import org.collectd.api.Collectd;
import org.collectd.api.ValueList;
import org.collectd.api.DataSet;
import org.collectd.api.DataSource;
import org.collectd.api.OConfigItem;

/**
 * Created by yonisha on 4/26/2015.
 */
public class ApplicationInsightsWriter implements
        CollectdConfigInterface,
        CollectdInitInterface,
        CollectdWriteInterface,
        CollectdShutdownInterface {

    // region Consts

    protected static final String UNDEFINED_HOST = "N/A";
    protected static final String METRIC_SOURCE_TAG_KEY = "MetricSource";
    protected static final String METRIC_SOURCE_TAG_VALUE = "CollectD-Plugin";
    protected static final String TELEMETRY_HOST_PROPERTY_NAME = "CollectD-Host";
    private static final int SUCCESS_CODE = 0;
    private static final int CONFIGURATION_PHASE_ERROR_CODE = 1;
    private static final int INITIALIZATION_PHASE_ERROR_CODE = 2;
    private static final String PLUGIN_NAME = "ApplicationInsightsWriter";
    private static final String DEFAULT_AI_LOGGER_OUTPUT = "CONSOLE";

    // endregion Consts

    // region Members

    private TelemetryConfiguration telemetryConfiguration;
    private TelemetryClient telemetryClient;
    private Map<String, PluginExclusion> excludedPluginsDictionary;
    private ApplicationInsightsWriterLogger logger = null;

    // endregion Members

    // region Ctors

    /**
     * Constructs new @ApplicationInsightsWriterLogger object.
     */
    public ApplicationInsightsWriter() {
        Collectd.registerConfig(PLUGIN_NAME, this);
        Collectd.registerInit(PLUGIN_NAME, this);
        Collectd.registerWrite(PLUGIN_NAME, this);
        Collectd.registerShutdown(PLUGIN_NAME, this);

        this.logger = new ApplicationInsightsWriterLogger();
        this.telemetryConfiguration = TelemetryConfiguration.getActive();
        this.excludedPluginsDictionary = new HashMap<String, PluginExclusion>();
    }

    /**
     * Constructs new @ApplicationInsightsWriterLogger object.
     * @param telemetryClient The telemetry client to use.
     * @param logger The logger to use.
     */
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
     * @return Zero on success, non-zero otherwise. Non-zero values will disable the plugin entirely.
     */
    public int config(OConfigItem configuration) {
        logger.logInfo("Loading configuration.");

        WriterConfiguration writerConfiguration = null;
        try {
            writerConfiguration = WriterConfiguration.buildConfiguration(configuration);
        } catch (ConfigurationException e) {
            logger.logError("Configuration failed, plugin will be disabled:\n" + e.toString());

            return CONFIGURATION_PHASE_ERROR_CODE;
        }

        this.telemetryConfiguration.setInstrumentationKey(writerConfiguration.getInstrumentationKey());
        if (writerConfiguration.getIsLoggerEnabled()) {
            InternalLogger.INSTANCE.initialize(DEFAULT_AI_LOGGER_OUTPUT, new HashMap<String, String>());
        }

        this.excludedPluginsDictionary = writerConfiguration.getPluginExclusions();
        logger.logInfo("Configuration loaded successfully.");

        return SUCCESS_CODE;
    }

    /**
     * Initializes the writer plugin.
     * @return Zero on success, non-zero otherwise. Non-zero values will disable the plugin entirely.
     */
    public int init() {
        logger.logInfo("Initializing...");

        try {
            this.telemetryClient = new TelemetryClient(this.telemetryConfiguration);

            logger.logInfo("Initialization completed.");
        } catch (Throwable e ) {
            logger.logError("Initialization failed, plugin will be disabled:\n" + e.toString());

            return INITIALIZATION_PHASE_ERROR_CODE;
        }

        return SUCCESS_CODE;
    }

    /**
     * Shutting down the writer plugin and flushes the telemetries from memory.
     * @return Zero on success, non-zero otherwise.
     */
    public int shutdown() {
        logger.logInfo("Flushing telemetries and shutting down...");
        this.telemetryClient.flush();

        return SUCCESS_CODE;
    }

    /**
     * Writes the given values to Application Insights.
     * @param valueList The values to be sent.
     * @return Zero on success, non-zero otherwise.
     */
    public int write(ValueList valueList) {
        // We're avoiding writing traces in this method to not spam the logs, as this method is being called very frequently.
        logger.logDebug("Writing values (" + valueList.getValues().size() + "):\n" + valueList.toString());

        String pluginName = valueList.getPlugin();
        for (int i = 0; i < valueList.getValues().size(); i++) {
            boolean dataSourceExcluded = isDataSourceExcluded(pluginName, valueList.getDataSet(), i);
            if (dataSourceExcluded) {
                continue;
            }

            MetricTelemetry metricTelemetry = createMetricTelemetry(valueList, i);
            this.telemetryClient.trackMetric(metricTelemetry);
        }

        return SUCCESS_CODE;
    }

    // endregion Public

    // region Private

    private static MetricTelemetry createMetricTelemetry(ValueList valueList, int index) {
        MetricTelemetry telemetry = new MetricTelemetry();

        DataSource dataSource = getDataSource(valueList.getDataSet(), index);
        String telemetryName = generateMetricName(valueList, dataSource);
        telemetry.setName(telemetryName);
        telemetry.getContext().getTags().put(METRIC_SOURCE_TAG_KEY, METRIC_SOURCE_TAG_VALUE);
        setHostMachineProperty(telemetry, valueList.getHost());

        Number value = valueList.getValues().get(index);
        if (value != null) {
            telemetry.setValue(value.doubleValue());
        }

        if (dataSource != null) {
            telemetry.setMin(dataSource.getMin());
            telemetry.setMax(dataSource.getMax());
        }

        return telemetry;
    }

    private static void setHostMachineProperty(MetricTelemetry telemetry, String host) {
        if (LocalStringsUtils.isNullOrEmpty(host)) {
            host = UNDEFINED_HOST;
        }

        telemetry.getProperties().put(TELEMETRY_HOST_PROPERTY_NAME, host);
    }

    private static DataSource getDataSource(DataSet dataSet, int index) {
        DataSource dataSource = null;
        if (dataSet != null) {
            dataSource = dataSet.getDataSources().get(index);
        }

        return dataSource;
    }

    private static String generateSourceName(ValueList valueList) {
        StringBuffer source = new StringBuffer();

        String plugin = valueList.getPlugin();
        if(!LocalStringsUtils.isNullOrEmpty(plugin)) {
            source.append('/').append(plugin);
        }

        String pluginInstance = valueList.getPluginInstance();
        if(!LocalStringsUtils.isNullOrEmpty(pluginInstance)) {
            source.append('/').append(pluginInstance);
        }

        String type = valueList.getType();
        if(!LocalStringsUtils.isNullOrEmpty(type)) {
            source.append('/').append(type);
        }

        String typeInstance = valueList.getTypeInstance();
        if(!LocalStringsUtils.isNullOrEmpty(typeInstance)) {
            source.append('/').append(typeInstance);
        }

        return source.toString();
    }

    protected static String generateMetricName(ValueList valueList, DataSource dataSource) {
        String metricName = generateSourceName(valueList);

        if (dataSource != null) {
            metricName = metricName.concat("/" + dataSource.getName());
        }

        return metricName;
    }

    private boolean isDataSourceExcluded(String pluginName, DataSet dataSet, int index) {
        PluginExclusion pluginExclusion = excludedPluginsDictionary.get(pluginName);
        if (pluginExclusion == null) {
            return false;
        }

        DataSource dataSource = getDataSource(dataSet, index);
        if (dataSource == null) {
            return false;
        }

        return pluginExclusion.isDataSourceExcluded(dataSource.getName());
    }

    // endregion Private
}
