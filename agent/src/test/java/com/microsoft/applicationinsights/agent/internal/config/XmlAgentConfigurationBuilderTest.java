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

package com.microsoft.applicationinsights.agent.internal.config;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public final class XmlAgentConfigurationBuilderTest {
    private final static String TEMP_TEST_FOLDER = "AgentTests";
    private final static String TEMP_CONF_FILE = "AI-Agent.xml";

    @Test
    public void testForbiddenSection() throws IOException {
        AgentConfiguration configuration = testConfiguration("ExcludedTest.xml");

        assertNotNull(configuration);

        Set<String> excludedPrefixes = configuration.getExcludedPrefixes();

        assertNotNull(excludedPrefixes);
        assertEquals(excludedPrefixes.size(), 2);
        assertTrue(excludedPrefixes.contains("a.AClass1"));
        assertTrue(excludedPrefixes.contains("a.b.AClass1"));
    }

    @Test
    public void testMalformedConfiguration() throws IOException {
        AgentConfiguration configuration = testConfiguration("MalformedTest.xml");
        assertNull(configuration);
    }

    @Test
    public void testClassesConfiguration() throws IOException {
        AgentConfiguration configuration = testConfiguration("InstrumentationTest.xml");
        Map<String, ClassInstrumentationData> classes = configuration.getRequestedClassesToInstrument();
        assertNotNull(classes);
        assertEquals(classes.size(), 2);
    }

    @Test
    public void testBuiltInConfiguration() throws IOException {
        AgentConfiguration configuration = testConfiguration("BuiltInTest.xml");
        AgentBuiltInConfiguration builtInConfiguration = configuration.getBuiltInConfiguration();
        assertEquals(builtInConfiguration.isEnabled(), true);
        assertEquals(builtInConfiguration.isHttpEnabled(), true);
        assertEquals(builtInConfiguration.isJdbcEnabled(), true);
        assertEquals(builtInConfiguration.isJdbcEnabled(), true);
        assertEquals(builtInConfiguration.isHibernateEnabled(), false);
    }

    @Test
    public void testLoggingConfiguration() throws IOException {
        AgentConfiguration configuration = testConfiguration("LoggingTest.xml");
        Map<String, String> loggingConfig = configuration.getAgentLoggingConfiguration();
        assertNotNull(loggingConfig);
        assertEquals("TRACE", loggingConfig.get("Level"));
        assertEquals("AI-Agent", loggingConfig.get("UniquePrefix"));
        assertEquals("C:/agent", loggingConfig.get("BaseFolderPath"));
        assertEquals("AIAGENT", loggingConfig.get("BaseFolder"));
    }

    private AgentConfiguration testConfiguration(String testFileName) throws IOException {
        File folder = null;
        try {
            folder = createFolder();
            ClassLoader classLoader = getClass().getClassLoader();
            URL testFileUrl = classLoader.getResource(testFileName);
            File sourceFile = new File(testFileUrl.toURI());
            File destinationFile = new File(folder, TEMP_CONF_FILE);
            FileUtils.copyFile(sourceFile, destinationFile);
            return new XmlAgentConfigurationBuilder().parseConfigurationFile(folder.toString());
        } catch (java.net.URISyntaxException e) {
            return null;
        } finally {
            cleanFolder(folder);
        }
    }

    private File createFolder() throws IOException {
        File folder;
        String filesPath = System.getProperty("java.io.tmpdir") + File.separator + TEMP_TEST_FOLDER;
        folder = new File(filesPath);
        if (folder.exists()) {
            try {
                FileUtils.deleteDirectory(folder);
            } catch (Exception e) {
            }
        }
        if (!folder.exists()) {
            folder.mkdir();
        }

        return folder;
    }

    private void cleanFolder(File folder) {
        if (folder != null && folder.exists()) {
            try {
                File file = new File(folder, TEMP_CONF_FILE);
                if (file.exists()) {
                    file.delete();
                }
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
            }
        }
    }
}