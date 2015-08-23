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

package com.microsoft.applicationinsights.internal.config;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.channel.stdout.StdOutChannel;

import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterConfigurationAware;
import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;
import com.microsoft.applicationinsights.internal.reflect.ClassDataVerifier;

import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class TelemetryConfigurationFactoryTest {

    private final static String MOCK_IKEY = "c9341531-05ac-4d8c-972e-36e97601d5ff";
    private final static String MOCK_ENDPOINT = "MockEndpoint";
    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";
    private final static String APP_INSIGHTS_IKEY_TEST_VALUE = "ds";

    @PerformanceModule
    static final class MockPerformanceModule implements TelemetryModule, PerformanceCounterConfigurationAware {
        public boolean initializeWasCalled = false;
        public boolean addConfigurationDataWasCalled = false;

        @Override
        public void initialize(TelemetryConfiguration configuration) {
            initializeWasCalled = true;
        }

        @Override
        public void addConfigurationData(PerformanceCountersXmlElement configuration) {
            addConfigurationDataWasCalled = true;
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
    public void configurationWithNullIkeyTest() {
        ikeyTest(null, null);
    }

    @Test
    public void configurationWithEmptykeyTest() {
        ikeyTest("", null);
    }

    @Test
    public void configurationWithBlankStringIkeyTest() {
        ikeyTest(" ", null);
    }

    @Test
    public void configurationWithRedundantSpacesIkeyTest() {
        ikeyTest(" " + MOCK_IKEY + " \t", MOCK_IKEY);
    }

    @Test
    public void configurationWithOnlyRedundantSpacesIkeyTest() {
        ikeyTest("  \t", null);
    }

    @Test
    public void systemPropertyIKeyBeforeConfigurationIKeyTest() {
        try {
            System.setProperty(TelemetryConfigurationFactory.EXTERNAL_PROPERTY_IKEY_NAME, APP_INSIGHTS_IKEY_TEST_VALUE);
            ikeyTest(MOCK_IKEY, APP_INSIGHTS_IKEY_TEST_VALUE);
        } finally {
            // Avoid any influence on other unit tests
            System.getProperties().remove(TelemetryConfigurationFactory.EXTERNAL_PROPERTY_IKEY_NAME);
        }
    }

    @Test
    public void testWithEmptySections() {
        AppInsightsConfigurationBuilder mockParser = Mockito.mock(AppInsightsConfigurationBuilder.class);

        ApplicationInsightsXmlConfiguration appConf = new ApplicationInsightsXmlConfiguration();
        appConf.setInstrumentationKey(MOCK_IKEY);
        appConf.setTelemetryInitializers(null);
        appConf.setContextInitializers(null);
        appConf.setModules(null);
        appConf.setSdkLogger(null);
        Mockito.doReturn(appConf).when(mockParser).build(any(InputStream.class));

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertEquals(mockConfiguration.getInstrumentationKey(), MOCK_IKEY);
        assertEquals(mockConfiguration.getContextInitializers().size(), 2);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testTelemetryContextInitializers() {
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build( null );
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
    public void testTelemetryModulesWithoutParameters() {
        MockTelemetryModule module = generateTelemetryModules(false);

        Assert.assertNotNull(module);
        Assert.assertNull(module.getParam1());
    }

    @Test
    public void testTelemetryModulesWithParameters() {
        MockTelemetryModule module = generateTelemetryModules(true);

        Assert.assertNotNull(module);
        Assert.assertEquals("value1", module.getParam1());
    }

    @Test
    public void testContextInitializers() {
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
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
        AppInsightsConfigurationBuilder mockParser = createMockParserThatFailsToParse();

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testInitializeWithNullGetInstrumentationKey() throws Exception {
        AppInsightsConfigurationBuilder mockParser = createMockParser(false, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
        appConf.setInstrumentationKey(MOCK_IKEY);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testInitializeWithEmptyGetInstrumentationKey() throws Exception {
        AppInsightsConfigurationBuilder mockParser = createMockParser(false, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
        appConf.setInstrumentationKey("");

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }

    @Test
    public void testInitializeAllDefaults() throws Exception {
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
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
        AppInsightsConfigurationBuilder mockParser = createMockParserWithDefaultChannel(true);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
        appConf.setInstrumentationKey(MOCK_IKEY);
        appConf.getChannel().setDeveloperMode(true);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.getChannel().isDeveloperMode(), true);
    }

    @Test
    public void testDefaultChannelWithBadData() {
        AppInsightsConfigurationBuilder mockParser = createMockParserWithDefaultChannel(true);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
        appConf.setInstrumentationKey(MOCK_IKEY);
        ChannelXmlElement channelXmlElement = appConf.getChannel();
        channelXmlElement.setEndpointAddress(NON_VALID_URL);
        channelXmlElement.setDeveloperMode(true);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.getChannel().isDeveloperMode(), false);
    }

    private MockTelemetryModule generateTelemetryModules(boolean addParameter) {
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
        appConf.setInstrumentationKey(MOCK_IKEY);

        TelemetryModulesXmlElement modulesXmlElement = new TelemetryModulesXmlElement();
        ArrayList<AddTypeXmlElement> modules = new ArrayList<AddTypeXmlElement>();
        AddTypeXmlElement addXmlElement = new AddTypeXmlElement();
        addXmlElement.setType("com.microsoft.applicationinsights.internal.config.MockTelemetryModule");

        if (addParameter) {
            final ParamXmlElement param1 = new ParamXmlElement();
            param1.setName("param1");
            param1.setValue("value1");

            ArrayList<ParamXmlElement> list = new ArrayList<ParamXmlElement>();
            list.add(param1);

            addXmlElement.setParameters(list);
        }

        modules.add(addXmlElement);
        modulesXmlElement.setAdds(modules);
        appConf.setModules(modulesXmlElement);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        MockTelemetryModule module = (MockTelemetryModule)mockConfiguration.getTelemetryModules().get(0);

        return module;
    }

    private AppInsightsConfigurationBuilder createMockParserThatFailsToParse() {
        AppInsightsConfigurationBuilder mockParser = Mockito.mock(AppInsightsConfigurationBuilder.class);
        Mockito.doReturn(null).when(mockParser).build(any(InputStream.class));
        return mockParser;
    }

    private AppInsightsConfigurationBuilder createMockParserWithDefaultChannel(boolean withChannel) {
        return createMockParser(withChannel, false, false);
    }

        // Suppress non relevant warning due to mockito internal stuff
    @SuppressWarnings("unchecked")
    private AppInsightsConfigurationBuilder createMockParser(boolean withChannel, boolean setChannel, boolean withPerformanceModules) {
        AppInsightsConfigurationBuilder mockParser = Mockito.mock(AppInsightsConfigurationBuilder.class);

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
        Mockito.doReturn(appConf).when(mockParser).build(any(InputStream.class));

        return mockParser;
    }

    @Test
    public void testPerformanceModules() {
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, true, true);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
        appConf.setInstrumentationKey(MOCK_IKEY);
        appConf.getChannel().setDeveloperMode(true);

        TelemetryConfigurationFactory.INSTANCE.setPerformanceCountersSection("com.microsoft.applicationinsights.internal.config");
        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.getTelemetryModules().size(), 1);
        assertTrue(mockConfiguration.getTelemetryModules().get(0) instanceof MockPerformanceModule);
        assertTrue(((MockPerformanceModule)mockConfiguration.getTelemetryModules().get(0)).initializeWasCalled);
        assertTrue(((MockPerformanceModule)mockConfiguration.getTelemetryModules().get(0)).addConfigurationDataWasCalled);
    }


    private void initializeWithFactory(AppInsightsConfigurationBuilder mockParser, TelemetryConfiguration mockConfiguration) {
        Field field = null;
        try {
            field = ClassDataUtils.class.getDeclaredField("verifier");
            field.setAccessible(true);

            ClassDataVerifier mockVerifier = Mockito.mock(ClassDataVerifier.class);
            Mockito.doReturn(true).when(mockVerifier).verifyClassExists(anyString());
            field.set(ClassDataUtils.INSTANCE, mockVerifier);
            TelemetryConfigurationFactory.INSTANCE.setBuilder(mockParser);
            TelemetryConfigurationFactory.INSTANCE.initialize(mockConfiguration);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException();
        }
    }

    private void ikeyTest(String configurationIkey, String expectedIkey) {
        // Make sure that there is no exception when fetching the i-key by having both
        // the i-key and channel in the configuration, otherwise the channel won't be instantiated
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, false, false);

        ApplicationInsightsXmlConfiguration appConf = new ApplicationInsightsXmlConfiguration();
        appConf.setInstrumentationKey(configurationIkey);
        Mockito.doReturn(appConf).when(mockParser).build(any(InputStream.class));

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);
        assertEquals(mockConfiguration.getInstrumentationKey(), expectedIkey);
        assertTrue(mockConfiguration.getChannel() instanceof InProcessTelemetryChannel);
    }
}
