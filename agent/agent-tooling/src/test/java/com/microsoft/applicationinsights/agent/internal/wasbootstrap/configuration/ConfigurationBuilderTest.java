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

package com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.ConfigurationBuilder.trimAndEmptyToNull;
import static org.junit.Assert.*;

public class ConfigurationBuilderTest {
    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    public Path getConfigFilePath(String resourceName) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        return file.toPath();
    }

    @Test
    public void testEmptyToNull() {
        assertEquals(null, trimAndEmptyToNull("   "));
        assertEquals(null, trimAndEmptyToNull(""));
        assertEquals(null, trimAndEmptyToNull(null));
        assertEquals("a", trimAndEmptyToNull("a"));
        assertEquals("a", trimAndEmptyToNull("  a  "));
        assertEquals(null, trimAndEmptyToNull("\t"));
    }

    @Test
    public void testGetConfigFilePath() {
        Path path = getConfigFilePath("applicationinsights.json");
        assertTrue(path.toString().endsWith("applicationinsights.json"));
    }

    @Test
    public void testValidJson() throws IOException {
        Path path = getConfigFilePath("applicationinsights.json");
        Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path, true);
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals("Something Good", configuration.role.name);
        assertEquals("xyz123", configuration.role.instance);
    }

    @Test
    public void testFaultyJson() throws IOException {
        Path path = getConfigFilePath("applicationinsights_faulty.json");
        Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path, true);
        // Configuration object should still be created.
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals("Something Good", configuration.role.name);
    }

    @Test(expected = FriendlyException.class)
    public void testMalformedJson() throws IOException {
        Path path = getConfigFilePath("applicationinsights_malformed.json");
        try {
            Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path, true);
            assertNull(configuration);
        } catch (FriendlyException ex) {
            assertTrue(ex.getMessage().contains("has a malformed JSON at path $.role."));
            throw ex;
        }
    }

    @Test(expected = FriendlyException.class)
    public void testMalformedFaultyJson() throws IOException {
        Path path = getConfigFilePath("applicationinsights_malformed_faulty.json");
        try {
            Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path, true);
            assertNull(configuration);
        } catch (FriendlyException ex) {
            System.out.println(ex.getMessage());
            assertTrue(ex.getMessage().contains("has a malformed JSON"));
            assertTrue(!ex.getMessage().contains("has a malformed JSON at path $.null."));
            throw ex;
        }
    }

    @Test
    public void testGetJsonEncodingExceptionMessage() {
        String pathNull = ConfigurationBuilder.getJsonEncodingExceptionMessage("file path/to/file",null);
        String pathEmpty = ConfigurationBuilder.getJsonEncodingExceptionMessage("file path/to/file","");
        String pathValid = ConfigurationBuilder.getJsonEncodingExceptionMessage("file path/to/file","has a malformed JSON at path $.role.");
        String pathInvalidAndNull = ConfigurationBuilder.getJsonEncodingExceptionMessage("file path/to/file","has a malformed JSON at path $.null.[0]");
        String pathInvalid = ConfigurationBuilder.getJsonEncodingExceptionMessage("file path/to/file","has a malformed JSON at path $.");
        assertEquals("Application Insights Java agent's configuration file path/to/file has a malformed JSON\n",pathNull);
        assertEquals("Application Insights Java agent's configuration file path/to/file has a malformed JSON\n",pathEmpty);
        assertEquals("Application Insights Java agent's configuration file path/to/file has a malformed JSON at path $.role.\n",pathValid);
        assertEquals("Application Insights Java agent's configuration file path/to/file has a malformed JSON\n",pathInvalid);
        assertEquals("Application Insights Java agent's configuration file path/to/file has a malformed JSON\n",pathInvalidAndNull);
    }
}
