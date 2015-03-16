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

package com.microsoft.applicationinsights.internal.config;

import java.util.*;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.channel.stdout.StdOutChannel;

import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;

public final class TelemetryConfigurationFactoryTest {

    private final static String MOCK_IKEY = "c9341531-05ac-4d8c-972e-36e97601d5ff";
    private final static String MOCK_ENDPOINT = "MockEndpoint";
    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";

    @PerformanceModule
    static final class MockPerformanceModule implements TelemetryModule {
        public boolean initializeWasCalled = false;

        @Override
        public void initialize(TelemetryConfiguration configuration) {
            initializeWasCalled = true;
        }
    }

    @PerformanceModule
    static final class MockPerformanceBadModule {
        public boolean initializeWasCalled = false;

        public void initialize(TelemetryConfiguration configuration) {
            initializeWasCalled = true;
        }
    }

    @Test
    public void testWithEmptySections() {
        AppInsightsConfigurationReader mockParser = Mockito.mock(AppInsightsConfigurationReader.class);

        ApplicationInsightsXmlConfiguration appConf = new ApplicationInsightsXmlConfiguration();
        appConf.setInstrumentationKey(MOCK_IKEY);
        appConf.setTelemetryInitializers(null);
        appConf.setContextInitializers(null);
        appConf.setModules(null);
        appConf.setSdkLogger(null);
        Mockito.doReturn(appConf).when(mockParser).build(anyString());

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertEquals(mockConfiguration.getInstrumentationKey(), MOCK_IKEY);
        assertEquals(mockConfiguration.getContextInitializers().size(), 2);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testTelemetryContextInitializers() {
        AppInsightsConfigurationReader mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey(MOCK_IKEY);

        TelemetryInitializersXmlElement telemetryInitializersXmlElement = new TelemetryInitializersXmlElement();
        ArrayList<AddTypeXmlElement> contexts = new ArrayList<AddTypeXmlElement>();
        AddTypeXmlElement addXmlElement = new AddTypeXmlElement();
        addXmlElement.setType("com.microsoft.applicationinsights.extensibility.initializer.TimestampPropertyInitializer");
        contexts.add(addXmlElement);
        telemetryInitializersXmlElement.setAdds(contexts);
        appConf.setTelemetryInitializers(telemetryInitializersXmlElement);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertEquals(mockConfiguration.getInstrumentationKey(), MOCK_IKEY);
        assertEquals(mockConfiguration.getContextInitializers().size(), 2);
        assertEquals(mockConfiguration.getTelemetryInitializers().size(), 1);
        assertTrue(mockConfiguration.getChannel() instanceof StdOutChannel);
    }

    @Test
    public void testContextInitializers() {
        AppInsightsConfigurationReader mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey(MOCK_IKEY);

        ContextInitializersXmlElement contextInitializersXmlElement = new ContextInitializersXmlElement();
        ArrayList<AddTypeXmlElement> contexts = new ArrayList<AddTypeXmlElement>();
        AddTypeXmlElement addXmlElement = new AddTypeXmlElement();
        addXmlElement.setType("com.microsoft.applicationinsights.extensibility.initializer.DeviceInfoContextInitializer");
        contexts.add(addXmlElement);
        contextInitializersXmlElement.setAdds(contexts);
        appConf.setContextInitializers(contextInitializersXmlElement);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertEquals(mockConfiguration.getInstrumentationKey(), MOCK_IKEY);
        assertEquals(mockConfiguration.getContextInitializers().size(), 3);
        assertTrue(mockConfiguration.getTelemetryInitializers().isEmpty());
        assertTrue(mockConfiguration.getChannel() instanceof StdOutChannel);
    }

    @Test
    public void testInitializeWithFailingParse() throws Exception {
        AppInsightsConfigurationReader mockParser = createMockParserThatFailsToParse();

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testInitializeWithNullGetInstrumentationKey() throws Exception {
        AppInsightsConfigurationReader mockParser = createMockParser(false, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey(MOCK_IKEY);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testInitializeWithEmptyGetInstrumentationKey() throws Exception {
        AppInsightsConfigurationReader mockParser = createMockParser(false, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey("");

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testInitializeAllDefaults() throws Exception {
        AppInsightsConfigurationReader mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey(MOCK_IKEY);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertEquals(mockConfiguration.getInstrumentationKey(), MOCK_IKEY);
        assertEquals(mockConfiguration.getContextInitializers().size(), 2);
        assertTrue(mockConfiguration.getTelemetryInitializers().isEmpty());
        assertTrue(mockConfiguration.getChannel() instanceof StdOutChannel);
    }

    @Test
    public void testDefaultChannelWithData() {
        AppInsightsConfigurationReader mockParser = createMockParserWithDefaultChannel(true);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey(MOCK_IKEY);
        appConf.getChannel().setDeveloperMode(true);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.getChannel().isDeveloperMode(), true);
    }

    @Test
    public void testDefaultChannelWithBadData() {
        AppInsightsConfigurationReader mockParser = createMockParserWithDefaultChannel(true);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey(MOCK_IKEY);
        ChannelXmlElement channelXmlElement = appConf.getChannel();
        channelXmlElement.setEndpointAddress(NON_VALID_URL);
        channelXmlElement.setDeveloperMode(true);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.getChannel().isDeveloperMode(), false);
    }

    private AppInsightsConfigurationReader createMockParserThatFailsToParse() {
        AppInsightsConfigurationReader mockParser = Mockito.mock(AppInsightsConfigurationReader.class);
        Mockito.doReturn(null).when(mockParser).build(anyString());
        return mockParser;
    }

    private AppInsightsConfigurationReader createMockParserWithDefaultChannel(boolean withChannel) {
        return createMockParser(withChannel, false, false);
    }

        // Suppress non relevant warning due to mockito internal stuff
    @SuppressWarnings("unchecked")
    private AppInsightsConfigurationReader createMockParser(boolean withChannel, boolean setChannel, boolean withPerformanceModules) {
        AppInsightsConfigurationReader mockParser = Mockito.mock(AppInsightsConfigurationReader.class);

        ApplicationInsightsXmlConfiguration appConf = new ApplicationInsightsXmlConfiguration();

        appConf.setSdkLogger(new SDKLoggerXmlElement());

        if (withChannel) {
            ChannelXmlElement channelXmlElement = new ChannelXmlElement();
            channelXmlElement.setEndpointAddress(MOCK_ENDPOINT);

            String channelType = null;
            if (setChannel) {
                channelType = "com.microsoft.applicationinsights.internal.channel.stdout.StdOutChannel";
                channelXmlElement.setType(channelType);
            }

            appConf.setChannel(channelXmlElement);
        }
        if (withPerformanceModules) {
            PerformanceCountersXmlElement performanceCountersXmlElement = new PerformanceCountersXmlElement();
            appConf.setPerformance(performanceCountersXmlElement);
        }
        Mockito.doReturn(appConf).when(mockParser).build(anyString());

        return mockParser;
    }

    @Test
    public void testPerformanceModules() {
        AppInsightsConfigurationReader mockParser = createMockParser(true, true, true);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build("");
        appConf.setInstrumentationKey(MOCK_IKEY);
        appConf.getChannel().setDeveloperMode(true);

        TelemetryConfigurationFactory.INSTANCE.setPerformanceCountersSection("com.microsoft.applicationinsights.internal.config");
        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.getTelemetryModules().size(), 1);
        assertTrue(mockConfiguration.getTelemetryModules().get(0) instanceof MockPerformanceModule);
        assertTrue(((MockPerformanceModule)mockConfiguration.getTelemetryModules().get(0)).initializeWasCalled);
    }


    private void initializeWithFactory(AppInsightsConfigurationReader mockParser, TelemetryConfiguration mockConfiguration) {
        TelemetryConfigurationFactory.INSTANCE.setBuilder(mockParser);
        TelemetryConfigurationFactory.INSTANCE.initialize(mockConfiguration);
    }
}