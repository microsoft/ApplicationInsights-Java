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

import java.util.Arrays;
import javax.naming.ConfigurationException;
import org.collectd.api.OConfigItem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Created by yonisha on 5/6/2015. */
public class WriterConfigurationTests {

  private static final String DEFAULT_INSTRUMENTATION_KEY = "00000000-0000-0000-0000-000000000000";
  private static OConfigItem defaultConfiguration;

  @BeforeClass
  public static void classInitialize() {
    WriterConfiguration.setLogger(new ApplicationInsightsWriterLogger(false));
  }

  private static OConfigItem createDefaultConfiguration() {
    OConfigItem instrumentationKeyConfigItem =
        new OConfigItem(WriterConfiguration.INSTRUMENTATION_KEY_CONFIGURATION_KEY);
    instrumentationKeyConfigItem.addValue(DEFAULT_INSTRUMENTATION_KEY);

    OConfigItem config = new OConfigItem("");
    config.addChild(instrumentationKeyConfigItem);

    return config;
  }

  @Before
  public void testInitialize() {
    defaultConfiguration = createDefaultConfiguration();
  }

  @Test
  public void testInstrumentationKeyParsedCorrectly() throws ConfigurationException {
    WriterConfiguration writerConfiguration =
        WriterConfiguration.buildConfiguration(defaultConfiguration);

    Assert.assertEquals(DEFAULT_INSTRUMENTATION_KEY, writerConfiguration.getInstrumentationKey());
  }

  @Test(expected = ConfigurationException.class)
  public void testInstrumentationKeyConfigurationWithNoActualValueThrowsException()
      throws ConfigurationException {
    OConfigItem instrumentationKeyConfigItem =
        new OConfigItem(WriterConfiguration.INSTRUMENTATION_KEY_CONFIGURATION_KEY);

    OConfigItem config = new OConfigItem("");
    config.addChild(instrumentationKeyConfigItem);

    WriterConfiguration.buildConfiguration(config);
  }

  @Test(expected = ConfigurationException.class)
  public void testIfInstrumentationKeyNotProvidedThenExceptionIsThrown()
      throws ConfigurationException {
    WriterConfiguration.buildConfiguration(new OConfigItem(""));
  }

  @Test
  public void testPluginAndDataSourceExclusions() throws ConfigurationException {
    String excludedPlugin = "Plugin";
    String excludedDS1 = "DS1";
    String excludedDS2 = "DS2";

    String excludeConfiguration =
        String.format("%s:%s,%s", excludedPlugin, excludedDS1, excludedDS2);

    OConfigItem exclude = new OConfigItem(WriterConfiguration.EXCLUDE_CONFIGURATION_KEY);
    exclude.addValue(excludeConfiguration);

    defaultConfiguration.addChild(exclude);
    WriterConfiguration writerConfiguration =
        WriterConfiguration.buildConfiguration(defaultConfiguration);

    PluginExclusion pluginExclusion = writerConfiguration.getPluginExclusions().get(excludedPlugin);

    Assert.assertTrue(pluginExclusion.isDataSourceExcluded(excludedDS1));
    Assert.assertTrue(pluginExclusion.isDataSourceExcluded(excludedDS2));
  }

  @Test
  public void testNoDataSourceProvidedForExclusion() throws ConfigurationException {
    String excludedPlugin = "Plugin";

    OConfigItem exclude = new OConfigItem(WriterConfiguration.EXCLUDE_CONFIGURATION_KEY);
    exclude.addValue(excludedPlugin);

    defaultConfiguration.addChild(exclude);
    WriterConfiguration writerConfiguration =
        WriterConfiguration.buildConfiguration(defaultConfiguration);

    PluginExclusion pluginExclusion = writerConfiguration.getPluginExclusions().get(excludedPlugin);

    Assert.assertTrue(pluginExclusion.isDataSourceExcluded("DS"));
  }

  @Test
  public void testLegitimateDataSourcesNotExcluded() {
    PluginExclusion pluginExclusion = new PluginExclusion("Plugin1", Arrays.asList("DS1", "DS2"));

    Assert.assertFalse(pluginExclusion.isDataSourceExcluded("DS3"));
  }
}
