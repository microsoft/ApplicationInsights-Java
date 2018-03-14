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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.HashSet;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Created by gupele on 5/25/2015.
 */
public final class ConfigurationFileLocator {

    /**
     * name of property containing path to directory with configuration file
     */
    public static final String CONFIG_DIR_PROPERTY = "applicationinsights.configurationDirectory";

    private final String configurationFileName;

    public ConfigurationFileLocator(String configurationFileName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(configurationFileName), "configurationFile should be non-null non empty value");
        this.configurationFileName = configurationFileName;
    }

    public InputStream getConfigurationFile() {

        String configurationFile;
        InputStream inputStream;

        // first try to get from dir defined explicitly in system property insights.configurationFile
        String configDirFromProperty = System.getProperty(CONFIG_DIR_PROPERTY);
        if (configDirFromProperty != null) {
            configurationFile = getConfigurationAbsolutePath(configDirFromProperty);
        } else {

            inputStream = ConfigurationFileLocator.class.getClassLoader().getResourceAsStream(configurationFileName);
            if (inputStream != null) {
                InternalLogger.INSTANCE.info("Configuration file has been successfully found as resource");
                return inputStream;
            }

            // Trying to load configuration as a resource.
            configurationFile = getConfigurationFromCurrentClassLoader();

            // If not found as a resource, trying to load from the executing jar directory
            if (configurationFile == null) {
                configurationFile = getConfigurationFromLibraryLocation();

                // If still not found try to get it from the class path
                if (configurationFile == null) {
                    configurationFile = getConfFromClassPath();
                }
            }
        }

        if (configurationFile != null) {
            InternalLogger.INSTANCE.info("Configuration file has been successfully found in: '%s'", configurationFile);
            try {
                return new FileInputStream(configurationFile);
            } catch (FileNotFoundException e) {
                InternalLogger.INSTANCE.warn("Configuration file '%s' could not be opened for reading, Exception : %s",
                        configurationFile,
                        ExceptionUtils.getStackTrace(e));
            }
        } else {
            InternalLogger.INSTANCE.error("Configuration file '%s' could not be found", configurationFileName);
        }
        return null;
    }

    private static void logException(Throwable t, String message) {
        if (t.getCause() != null) {
            InternalLogger.INSTANCE.warn("Failed to find configuration file, exception while fetching from %s: " +
                            "Exception : '%s'",
                    message, ExceptionUtils.getStackTrace(t));
        } else {
            InternalLogger.INSTANCE.warn("Failed to find configuration file, exception while fetching from %s: " +
                            "Exception : '%s'",
                    message, ExceptionUtils.getStackTrace(t));
        }
    }

    private String getConfigurationFromCurrentClassLoader() {
        try {
            ClassLoader classLoader = ConfigurationFileLocator.class.getClassLoader();
            String configurationFile = null;
            URL resourceUrl = classLoader.getResource(configurationFileName);
            if (resourceUrl != null) {
                File filePath = normalizeUrlToFile(resourceUrl);
                if (filePath != null) {
                    configurationFile = filePath.toString();
                }
            }

            InternalLogger.INSTANCE.info(
                    "Configuration file '%s' was %s found by default class loader",
                    configurationFileName,
                    configurationFile == null ? "NOT " : "");

            return configurationFile;
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                logException(t, "current class loader");
                InternalLogger.INSTANCE.trace("stack trace is : %s", ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }

        return null;
    }

    private String getConfigurationFromLibraryLocation() {
        try {
            CodeSource codeSource = ConfigurationFileLocator.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }

            String jarFullPath = codeSource.getLocation().toURI().getPath();
            File jarFile = new File(jarFullPath);

            if (jarFile.exists()) {
                String jarDirectory = jarFile.getParent();
                String configurationPath = getConfigurationAbsolutePath(jarDirectory);
                if (configurationPath != null) {
                    return configurationPath;
                }
            } else {
                InternalLogger.INSTANCE.warn("Can not access folder '%s'", jarFullPath);
            }
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                logException(t, "library location");
                InternalLogger.INSTANCE.trace("stack trace is : %s", ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
        return null;
    }

    private String getConfFromClassPath() {
        try {
            ClassLoader classLoader = TelemetryConfigurationFactory.class.getClassLoader();
            if (!(classLoader instanceof URLClassLoader)) {
                return null;
            }

            HashSet<String> checkedUrls = new HashSet<String>();

            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            for (URL url : urls) {
                File filePath = normalizeUrlToFile(url);

                if (filePath == null) {
                    continue;
                }

                if (filePath.isFile()) {
                    filePath = filePath.getParentFile();
                }

                String configurationPath = filePath.toString();

                if (checkedUrls.contains(configurationPath)) {
                    continue;
                }

                String configurationFile = getConfigurationAbsolutePath(configurationPath);
                if (configurationFile != null) {
                    return configurationFile;
                } else {
                    checkedUrls.add(configurationPath);
                }
            }
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                logException(t, "class path");
                InternalLogger.INSTANCE.trace("stack trace is : %s", ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
        return null;
    }

    private String getConfigurationAbsolutePath(String path) {
        File configFile = new File(path, configurationFileName);

        if (configFile.exists()) {
            return configFile.getAbsolutePath();
        }

        InternalLogger.INSTANCE.info("Did not find configuration file '%s' in '%s'", configurationFileName, path);


        return null;
    }

    private File normalizeUrlToFile(URL url) {
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            InternalLogger.INSTANCE.warn("Failed to convert URL '%s' to URI, Exception : %s ", url,
                    ExceptionUtils.getStackTrace(e));
            return null;
        }

        return new File(uri.getSchemeSpecificPart());
    }
}
