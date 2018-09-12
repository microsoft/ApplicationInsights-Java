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
import java.lang.reflect.Method;
import java.util.*;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.channel.concrete.nop.NopTelemetryChannel;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.channel.stdout.StdOutChannel;

import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;
import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatModule;
import com.microsoft.applicationinsights.internal.perfcounter.JvmPerformanceCountersModule;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterConfigurationAware;
import com.microsoft.applicationinsights.internal.perfcounter.ProcessPerformanceCountersModule;
import com.microsoft.applicationinsights.internal.processor.RequestTelemetryFilter;
import com.microsoft.applicationinsights.internal.processor.SyntheticSourceFilter;
import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;
import com.microsoft.applicationinsights.internal.reflect.ClassDataVerifier;

import org.hamcrest.Matchers;
import org.junit.*;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import static org.junit.Assert.*;

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
    public void systemPropertyIKeySecondaryBeforeConfigurationIKeyTest() {
        try {
            System.setProperty(TelemetryConfigurationFactory.EXTERNAL_PROPERTY_IKEY_NAME_SECONDARY, APP_INSIGHTS_IKEY_TEST_VALUE);
            ikeyTest(MOCK_IKEY, APP_INSIGHTS_IKEY_TEST_VALUE);
        } finally {
            // Avoid any influence on other unit tests
            System.getProperties().remove(TelemetryConfigurationFactory.EXTERNAL_PROPERTY_IKEY_NAME_SECONDARY);
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
        assertThat(mockConfiguration.getContextInitializers(), hasSize(3));
        assertThat(mockConfiguration.getChannel(), instanceOf(InProcessTelemetryChannel.class));
    }

    @Test
    public void testTelemetryProcessors() {
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, true, false);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build( null );
        appConf.setInstrumentationKey(MOCK_IKEY);

        TelemetryProcessorsXmlElement telemetryProcessorsXmlElement = new TelemetryProcessorsXmlElement();

        TelemetryProcessorXmlElement builtIn = new TelemetryProcessorXmlElement();
        builtIn.setType("SyntheticSourceFilter");
        telemetryProcessorsXmlElement.getBuiltInTelemetryProcessors().add(builtIn);

        builtIn = new TelemetryProcessorXmlElement();
        builtIn.setType("RequestTelemetryFilter");
        ParamXmlElement prop = new ParamXmlElement();
        prop.setName("MinimumDurationInMS");
        prop.setValue("100");
        builtIn.getAdds().add(prop);

        prop = new ParamXmlElement();
        prop.setName("NotNeededResponseCodes");
        prop.setValue("100-400,500");
        builtIn.getAdds().add(prop);
        telemetryProcessorsXmlElement.getBuiltInTelemetryProcessors().add(builtIn);

        TelemetryProcessorXmlElement custom = new TelemetryProcessorXmlElement();

        custom.setType("com.microsoft.applicationinsights.internal.config.TestProcessorThatThrowsOnSetter");
        prop = new ParamXmlElement();
        prop.setName("Property");
        prop.setValue("value");
        custom.getAdds().add(prop);
        telemetryProcessorsXmlElement.getCustomTelemetryProcessors().add(custom);

        custom = new TelemetryProcessorXmlElement();
        custom.setType("com.microsoft.applicationinsights.internal.config.ValidProcessorsWithSetters");
        prop = new ParamXmlElement();
        prop.setName("PropertyA");
        prop.setValue("valueA");
        custom.getAdds().add(prop);

        prop = new ParamXmlElement();
        prop.setName("PropertyB");
        prop.setValue("valudeA");
        custom.getAdds().add(prop);
        telemetryProcessorsXmlElement.getCustomTelemetryProcessors().add(custom);

        custom = new TelemetryProcessorXmlElement();
        custom.setType("com.microsoft.applicationinsights.internal.config.TestProcessorWithoutSetters");
        telemetryProcessorsXmlElement.getCustomTelemetryProcessors().add(custom);

        appConf.setTelemetryProcessors(telemetryProcessorsXmlElement);

        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        assertEquals(mockConfiguration.isTrackingDisabled(), false);
        assertEquals(mockConfiguration.getInstrumentationKey(), MOCK_IKEY);
        assertThat(mockConfiguration.getContextInitializers(), hasSize(3));
        assertThat(mockConfiguration.getTelemetryInitializers(), empty());
        assertThat(mockConfiguration.getChannel(), instanceOf(StdOutChannel.class));

        assertThat(mockConfiguration.getTelemetryProcessors(), hasSize(4));

        assertThat(mockConfiguration.getTelemetryProcessors(), Matchers.<TelemetryProcessor>hasItem(instanceOf(SyntheticSourceFilter.class)));
        assertThat(mockConfiguration.getTelemetryProcessors(), Matchers.<TelemetryProcessor>hasItem(instanceOf(RequestTelemetryFilter.class)));
        assertThat(mockConfiguration.getTelemetryProcessors(), Matchers.<TelemetryProcessor>hasItem(instanceOf(ValidProcessorsWithSetters.class)));
        assertThat(mockConfiguration.getTelemetryProcessors(), Matchers.<TelemetryProcessor>hasItem(instanceOf(TestProcessorWithoutSetters.class)));
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
        assertThat(mockConfiguration.getContextInitializers(), hasSize(3));
        assertThat(mockConfiguration.getTelemetryInitializers(), hasSize(1));
        assertThat(mockConfiguration.getTelemetryProcessors(), empty());
        assertThat(mockConfiguration.getChannel(), instanceOf(StdOutChannel.class));
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
        assertThat(mockConfiguration.getContextInitializers(), hasSize(4));
        assertThat(mockConfiguration.getTelemetryInitializers(), empty());
        assertThat(mockConfiguration.getTelemetryProcessors(), empty());
        assertThat(mockConfiguration.getChannel(), instanceOf(StdOutChannel.class));
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
        assertThat(mockConfiguration.getContextInitializers(), Matchers.<ContextInitializer>hasSize(3));
        assertThat(mockConfiguration.getTelemetryInitializers(), Matchers.<TelemetryInitializer>empty());
        assertThat(mockConfiguration.getTelemetryProcessors(), empty());
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

    @Test
    public void testEmptyConfiguration() {
        TelemetryConfiguration emptyConfig = TelemetryConfiguration.getActiveWithoutInitializingConfig();
        Assert.assertEquals(null, emptyConfig.getInstrumentationKey());
        Assert.assertSame(NopTelemetryChannel.instance(), emptyConfig.getChannel());
        Assert.assertEquals(0, emptyConfig.getTelemetryModules().size());
        Assert.assertEquals(false, emptyConfig.isTrackingDisabled());
        Assert.assertEquals(0, emptyConfig.getContextInitializers().size());
        Assert.assertEquals(0, emptyConfig.getTelemetryProcessors().size());
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
    public void testPerformanceModules_WithScanningEnabled() {
        final String prevPropValue = System.getProperty(TelemetryConfigurationFactory.PERFORMANCE_MODULES_SCANNING_ENABLED_PROPERTY);
        try {
            List<TelemetryModule> mods = doPerfModuleTest("true", 2);

            assertTrue(Iterables.any(mods, Predicates.instanceOf(HeartBeatModule.class)));
            assertTrue(Iterables.any(mods, Predicates.instanceOf(MockPerformanceModule.class)));
            MockPerformanceModule theModule = Iterables.getOnlyElement(Iterables.filter(mods, MockPerformanceModule.class));
            assertTrue(theModule.initializeWasCalled);
            assertTrue(theModule.addConfigurationDataWasCalled);
        } finally {
            restoreProperties(prevPropValue);
        }
    }

//    @Ignore
    @Test
    public void testPerformanceModules_withScanningDisabled() {
        final String prevPropValue = System.getProperty(TelemetryConfigurationFactory.PERFORMANCE_MODULES_SCANNING_ENABLED_PROPERTY);
        try {
            List<TelemetryModule> mods = doPerfModuleTest("false", 3);
            assertTrue(Iterables.any(mods, Predicates.instanceOf(HeartBeatModule.class)));
            assertTrue(Iterables.any(mods, Predicates.instanceOf(JvmPerformanceCountersModule.class)));
            assertTrue(Iterables.any(mods, Predicates.instanceOf(ProcessPerformanceCountersModule.class)));
        } finally {
            restoreProperties(prevPropValue);
        }
    }

    private void restoreProperties(String prevPropValue) {
        if (prevPropValue == null) {
            final Properties properties = System.getProperties();
            properties.remove(TelemetryConfigurationFactory.PERFORMANCE_MODULES_SCANNING_ENABLED_PROPERTY);
            System.setProperties(properties);
        } else {
            System.setProperty(TelemetryConfigurationFactory.PERFORMANCE_MODULES_SCANNING_ENABLED_PROPERTY, prevPropValue);
        }
    }

    private List<TelemetryModule> doPerfModuleTest(String enabled, int expectedModuleCount) {
        AppInsightsConfigurationBuilder mockParser = createMockParser(true, true, true);
        ApplicationInsightsXmlConfiguration appConf = mockParser.build(null);
        appConf.setInstrumentationKey(MOCK_IKEY);
        appConf.getChannel().setDeveloperMode(true);

        System.setProperty(TelemetryConfigurationFactory.PERFORMANCE_MODULES_SCANNING_ENABLED_PROPERTY, enabled);
        TelemetryConfigurationFactory.INSTANCE.setPerformanceCountersSection("com.microsoft.applicationinsights.internal.config");
        TelemetryConfiguration mockConfiguration = new TelemetryConfiguration();

        initializeWithFactory(mockParser, mockConfiguration);

        //heartbeat is added as default
        List<TelemetryModule> mods = mockConfiguration.getTelemetryModules();
        for (TelemetryModule tm : mods) {
            System.out.println(" >> loaded telemetry module: "+tm.getClass().getCanonicalName());
        }
        assertEquals(expectedModuleCount, mods.size());
        return mods;
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

    @After
    public void tearDown() throws Exception {
        Method method = TelemetryConfiguration.class.getDeclaredMethod("setActiveAsNull");
        method.setAccessible(true);
        method.invoke(null);
    }
}
