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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.collectd.internal.ApplicationInsightsWriterLogger;
import com.microsoft.applicationinsights.collectd.internal.WriterConfiguration;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.collectd.api.*;
import org.junit.*;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by yonisha on 5/3/2015.
 */
public class ApplicationInsightsWriterTests {

    private static final String DEFAULT_INSTRUMENTATION_KEY = "00000000-0000-0000-0000-000000000000";
    private static final String DATA_SET_TYPE = "Type";
    private static final String HOST = "Host";
    private static final String PLUGIN = "Plugin";
    private static final String PLUGIN_INSTANCE = "PInstance";
    private static final String DEFAULT_DATA_SOURCE = "Default_Source";
    private static final String SECOND_DATA_SOURCE = "Second_Source";

    private static List<DataSource> dataSources = null;

    private static ValueList defaultValueList;
    private TelemetryClient telemetryClient;
    private List<MetricTelemetry> telemetriesSent;
    private ApplicationInsightsWriter writerUnderTest;
    private OConfigItem defaultConfiguration;

    @BeforeClass
    public static void classInitialize() {
        WriterConfiguration.setLogger(new ApplicationInsightsWriterLogger(false));
        defaultValueList = new ValueList();
        defaultValueList.setHost(HOST);
        defaultValueList.setType(DATA_SET_TYPE);
        defaultValueList.setPlugin(PLUGIN);
        defaultValueList.setPluginInstance(PLUGIN_INSTANCE);

        List<DataSource> dataSources = initializeDataSources();
        DataSet dataSet = new DataSet(DATA_SET_TYPE, dataSources);
        dataSet.setType(DATA_SET_TYPE);

        defaultValueList.setDataSet(dataSet);
        defaultValueList.addValue(0);
        defaultValueList.addValue(1);
    }

    @Before
    public void testInitialize() {
        this.telemetryClient = mock(TelemetryClient.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MetricTelemetry telemetry = (MetricTelemetry) invocation.getArguments()[0];
                telemetriesSent.add(telemetry);

                return null;
            }
        }).when(this.telemetryClient).trackMetric(Matchers.any(MetricTelemetry.class));

        defaultConfiguration = initializeConfiguration();
        this.writerUnderTest = new ApplicationInsightsWriter(this.telemetryClient, new ApplicationInsightsWriterLogger(false));
        this.writerUnderTest.config(defaultConfiguration);
        this.telemetriesSent = new ArrayList<MetricTelemetry>();
    }

    @Test
    public void testValuesSentCorrectly() {
        this.writerUnderTest.write(defaultValueList);

        Assert.assertEquals(2, this.telemetriesSent.size());
        verifySentTelemetries();
    }

    @Test
    public void testTelemetriesAreFlushedOnShutdown() {
        this.writerUnderTest.shutdown();

        verify(this.telemetryClient).flush();
    }

    @Test
    public void testInstrumentationKeyIsSetWhenCallingConfig() {
        this.writerUnderTest.config(defaultConfiguration);

        Assert.assertEquals(DEFAULT_INSTRUMENTATION_KEY, TelemetryConfiguration.getActive().getInstrumentationKey());
    }

    @Test
    public void testExcludedDataSourceNotSendingEvent() {
        OConfigItem exclusionConfiguration = new OConfigItem(WriterConfiguration.EXCLUDE_CONFIGURATION_KEY);
        exclusionConfiguration.addValue(PLUGIN + ":" + DEFAULT_DATA_SOURCE);
        defaultConfiguration.addChild(exclusionConfiguration);

        this.writerUnderTest.config(defaultConfiguration);
        this.writerUnderTest.write(defaultValueList);

        Assert.assertEquals(1, this.telemetriesSent.size());

        MetricTelemetry telemetry = this.telemetriesSent.get(0);
        Assert.assertTrue(telemetry.getName().contains(SECOND_DATA_SOURCE));
    }

    @Test
    public void testExcludedPluginNotSendingEvent() {
        OConfigItem exclusionConfiguration = new OConfigItem(WriterConfiguration.EXCLUDE_CONFIGURATION_KEY);
        exclusionConfiguration.addValue(PLUGIN);
        defaultConfiguration.addChild(exclusionConfiguration);

        this.writerUnderTest.config(defaultConfiguration);
        this.writerUnderTest.write(defaultValueList);

        Assert.assertEquals(0, this.telemetriesSent.size());
    }

    private void verifySentTelemetries() {
        for (int i = 0; i < telemetriesSent.size(); i++) {
            DataSource dataSource = dataSources.get(i);
            MetricTelemetry telemetrySent = telemetriesSent.get(i);
            String expectedMetricName = ApplicationInsightsWriter.generateMetricName(defaultValueList, dataSource);

            Assert.assertEquals(expectedMetricName, telemetrySent.getName());
            Assert.assertEquals(i, (int)telemetrySent.getValue());
            Assert.assertEquals(dataSource.getMin(), telemetrySent.getMin(), 0);
            Assert.assertEquals(dataSource.getMax(), telemetrySent.getMax(), 0);
        }
    }

    private static OConfigItem initializeConfiguration() {
        OConfigItem instrumentationKeyConfigItem = new OConfigItem(WriterConfiguration.INSTRUMENTATION_KEY_CONFIGURATION_KEY);
        instrumentationKeyConfigItem.addValue(DEFAULT_INSTRUMENTATION_KEY);

        OConfigItem config = new OConfigItem("");
        config.addChild(instrumentationKeyConfigItem);

        return config;
    }

    private static List<DataSource> initializeDataSources() {
        DataSource firstDataSource = new DataSource(DEFAULT_DATA_SOURCE, 1, 5, 15);
        DataSource secondDataSource= new DataSource(SECOND_DATA_SOURCE, 1, 25, 35);

        dataSources = new ArrayList<DataSource>();
        dataSources.add(firstDataSource);
        dataSources.add(secondDataSource);

        return dataSources;
    }
}
