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

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.*;

public final class ConfigurationFileLocatorTest {
    private final static String MOCK_CONF_FILE = "MockApplicationInsights.xml";
    private final static String EXISTING_CONF_TEST_FILE = "ApplicationInsights.xml";

    @Test(expected = IllegalArgumentException.class)
    public void testCtorWithNull() {
        new ConfigurationFileLocator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtorWithEmpty() {
        new ConfigurationFileLocator("");
    }

    @Test
    public void testGetConfigurationFileWhereFileIsResource() throws Exception {
        String configurationFileName = putConfigurationFileAsResourceInCurrentClassLoaderOnly();

        configurationFileName = new ConfigurationFileLocator(configurationFileName).getConfigurationFile();
        verifyFile(configurationFileName);
    }

    @Test
    public void testGetConfigurationFileWhereFileIsJarLocationOnly() throws Exception {
        String configurationFileName = putConfigurationFileInLibraryLocationOnly();

        configurationFileName = new ConfigurationFileLocator(configurationFileName).getConfigurationFile();
        verifyFile(configurationFileName);
    }

    @Test
    public void testGetConfigurationFileWhereFileIsInClassPathOnly() throws Exception {
        String configurationFileName = putConfigurationFileAsResourceInCurrentClassLoaderOnly();

        configurationFileName = new ConfigurationFileLocator(configurationFileName).getConfigurationFile();
        verifyFile(configurationFileName);
    }

    @Test
    public void testGetConfigurationFileWhereFileInBothClassPathAndJarLocation() throws Exception {
        putConfigurationFileInClassPathAndJarLocation(MOCK_CONF_FILE, MOCK_CONF_FILE);

        String configurationFileName = new ConfigurationFileLocator(MOCK_CONF_FILE).getConfigurationFile();
        verifyFile(configurationFileName);
    }

    @Test
    public void testGetConfigurationFileJarLocationIsfoundFirst() throws Exception {
        putConfigurationFileInClassPathAndJarLocation("dontfind" + MOCK_CONF_FILE, MOCK_CONF_FILE);

        String configurationFileName = new ConfigurationFileLocator(MOCK_CONF_FILE).getConfigurationFile();
        verifyFile(configurationFileName);
    }

    private String putConfigurationFileInClassPathOnly() throws URISyntaxException, IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        eraseFromLibraryLocation();

        putFileInClassPath(MOCK_CONF_FILE);

        return MOCK_CONF_FILE;
    }

    private void putFileInClassPath(String configurationFileName) throws URISyntaxException, NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        ClassLoader classLoader = ConfigurationFileLocator.class.getClassLoader();
        if (!(classLoader instanceof URLClassLoader)) {
            return;
        }

        File file = getMockApplicationFileFromClassPath(classLoader);
        if (file == null) {
            String path = ConfigurationFileLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

            Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL",new Class[]{URL.class});
            addURLMethod.setAccessible(true);
            addURLMethod.invoke(classLoader, new Object[]{new File(path).toURI().toURL()});

            file = new File(path, configurationFileName);
            file.createNewFile();
        }

        file.deleteOnExit();
    }

    private String putConfigurationFileInLibraryLocationOnly() throws URISyntaxException, IOException {
        eraseFromClassPath();

        putConfigurationInLibraryLocation(MOCK_CONF_FILE);

        return MOCK_CONF_FILE;
    }

    private void putConfigurationInLibraryLocation(String configurationFileName) throws URISyntaxException, IOException {
        String jarFullPath = ConfigurationFileLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        File configFile = new File(new File(jarFullPath).getParent(), configurationFileName);

        configFile.createNewFile();

        configFile.deleteOnExit();
    }

    private void putConfigurationFileInClassPathAndJarLocation(String confForClassPath, String confForJarLocation)
            throws URISyntaxException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        putFileInClassPath(confForClassPath);
        putConfigurationInLibraryLocation(confForJarLocation);
    }

    private String putConfigurationFileAsResourceInCurrentClassLoaderOnly() throws URISyntaxException {
        eraseFromLibraryLocation();
        eraseFromClassPath();

        return EXISTING_CONF_TEST_FILE;
    }

    private void eraseFromLibraryLocation() throws URISyntaxException {
        String jarFullPath = ConfigurationFileLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        File configFile = new File(new File(jarFullPath).getParent(), MOCK_CONF_FILE);
        configFile.delete();
    }

    private void eraseFromClassPath() throws URISyntaxException {
        ClassLoader classLoader = ConfigurationFileLocator.class.getClassLoader();
        if (!(classLoader instanceof URLClassLoader)) {
            return;
        }

        File file = getMockApplicationFileFromClassPath(classLoader);
        if (file != null) {
            file.delete();
        }
    }

    private File getMockApplicationFileFromClassPath(ClassLoader classLoader) throws URISyntaxException {
        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        for (URL url : urls) {
            URI uri = url.toURI();
            File file = new File(uri.getSchemeSpecificPart());
            if (file.isDirectory()) {
                continue;
            }

            if (MOCK_CONF_FILE.equals(file.getName())) {
                return file;
            }
        }

        return null;
    }

    private void verifyFile(String confFileName) {
        assertNotNull("Configuration file is not found in the jar location", confFileName);

        File confFile = new File(confFileName);
        assertTrue("Returned value is not a file", confFile.isFile());
        assertTrue("Returned value does not exist", confFile.exists());
    }
}
