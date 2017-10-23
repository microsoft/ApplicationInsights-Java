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

package com.microsoft.applicationinsights.agent.internal.agent;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.microsoft.applicationinsights.agent.internal.agent.jmx.JmxConnectorLoader;
import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfigurationBuilderFactory;
import com.microsoft.applicationinsights.agent.internal.config.DataOfConfigurationForException;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

/**
 * Created by gupele on 5/6/2015.
 */
public final class AgentImplementation {

    private final static String AGENT_JAR_PREFIX = "applicationinsights-agent";
    private final static String CORE_JAR_PREFIX = "applicationinsights-core";
    private final static String DISTRIBUTION_JAR_PREFIX = "applicationinsights-all";
    private final static String CORE_SELF_REGISTRATOR_CLASS_NAME = "com.microsoft.applicationinsights.internal.agent.AgentSelfConnector";
    private final static String CORE_SELF_SHORT_REGISTRATOR_CLASS_NAME = "AgentSelfConnector";

    private static String agentJarLocation;

    public static void premain(String args, Instrumentation inst) {

        try {
            agentJarLocation = getAgentJarLocation();
            appendJarsToBootstrapClassLoader(inst);
            initializeCodeInjector(inst);
        } catch (Throwable throwable) {
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Agent is NOT activated: failed to load to bootstrap class loader: " + throwable.getMessage());
			throwable.printStackTrace();
            System.exit(-1);
        }
    }

    @SuppressWarnings("unchecked")
    private static void initializeCodeInjector(Instrumentation inst) throws Throwable {
        ClassLoader bcl = AgentImplementation.class.getClassLoader().getParent();
        Class<CodeInjector> cic = (Class<CodeInjector>) bcl.loadClass("com.microsoft.applicationinsights.agent.internal.agent.CodeInjector");
        if (cic == null) {
            throw new IllegalStateException("Failed to load CodeInjector");
        }

        AgentConfiguration agentConfiguration = new AgentConfigurationBuilderFactory().createDefaultBuilder().parseConfigurationFile(agentJarLocation);

        if (agentConfiguration.isSelfRegistrationMode()) {
            SetNonWebAppModeIfAskedByConf(agentConfiguration.getSdkPath());
        }

        try {
            CodeInjector codeInjector = cic.getDeclaredConstructor(AgentConfiguration.class).newInstance(agentConfiguration);

            DataOfConfigurationForException exceptionData = agentConfiguration.getBuiltInConfiguration().getDataOfConfigurationForException();
            if (inst.isRetransformClassesSupported()) {
                if (exceptionData.isEnabled()) {
                    InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.TRACE, "Instrumenting runtime exceptions.");

                    inst.addTransformer(codeInjector, true);
                    ImplementationsCoordinator.INSTANCE.setExceptionData(exceptionData);
                    inst.retransformClasses(RuntimeException.class);
                    inst.removeTransformer(codeInjector);
                }
			} else {
                if (exceptionData.isEnabled()) {
                    InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.TRACE, "The JVM does not support re-transformation of classes.");
                }
			}
            inst.addTransformer(codeInjector);
        } catch (Exception e) {
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Failed to load the code injector, exception: %s", e.getMessage());
            throw e;
        }
    }

    private static void appendJarsToBootstrapClassLoader(Instrumentation inst) throws Throwable {
        String agentJarPath = agentJarLocation.startsWith("file:/") ? agentJarLocation : new File(agentJarLocation).toURI().toString();

        String agentJarName = null;
        File agentFolder = new File(agentJarLocation);
        for (File file : agentFolder.listFiles()) {
            if (file.getName().indexOf(AGENT_JAR_PREFIX) != -1) {
                agentJarName = file.getName();
                break;
            }
        }

        if (agentJarName == null) {
            throw new RuntimeException("Could not find agent jar");
        }

        InternalAgentLogger.INSTANCE.info("Found jar: " + agentJarPath + " " + agentJarName);

        URL configurationURL = new URL(agentJarPath + agentJarName);

        JarFile agentJar = new JarFile(URLDecoder.decode(configurationURL.getFile(), "UTF-8"));

        inst.appendToBootstrapClassLoaderSearch(agentJar);

        InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.TRACE, "Successfully loaded Agent jar");
    }

    public static String getAgentJarLocation() throws UnsupportedEncodingException {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try {
            if ((systemClassLoader instanceof URLClassLoader)) {
                for (URL url : ((URLClassLoader)systemClassLoader).getURLs()) {
                    String urlPath = url.getPath();
                    if (urlPath.charAt(0) == '/') {
                        urlPath = urlPath.substring(1);
                    }
                    Path path = Paths.get(urlPath);
                    if (path.getFileName().toString().startsWith(AGENT_JAR_PREFIX)) {
                        return path.getParent().toString();
                    }
                }
            }
        } catch (Throwable throwable) {
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Error while trying to fetch Jar Location, Exception: " + throwable.getMessage());
        }

        String stringPath = AgentImplementation.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (stringPath.charAt(0) == '/') {
            stringPath = stringPath.substring(1);
        }
        Path path = Paths.get(stringPath);
        if (path.getFileName().toString().startsWith(AGENT_JAR_PREFIX)) {
            return path.getParent().toString();
        }
        else {
            InternalAgentLogger.INSTANCE.error("Cannot find applicationinsights-agent jar, agent cannot be loaded");
        }
        return path.getParent().toString();
    }

    private static void SetNonWebAppModeIfAskedByConf(String sdkPath) throws Throwable {
        String path = sdkPath;
        if (StringUtils.isNullOrEmpty(path)) {
            path = agentJarLocation;
        }
        File sdkFolder = new File(path);
        if (!sdkFolder.exists()) {
            String errorMessage = String.format("Path %s for core jar does not exist", path);
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, errorMessage);
            throw new Exception(errorMessage);
        }

        if (!sdkFolder.isDirectory()) {
            String errorMessage = String.format("Path %s for core jar must be a folder", path);
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, errorMessage);
            throw new Exception(errorMessage);
        }

        if (!sdkFolder.canRead()) {
            String errorMessage = String.format("Path %s for core jar must be a folder that can be read", path);
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, errorMessage);
            throw new Exception(errorMessage);
        }

        InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.TRACE, "Found %s", path);
        String coreJarName = null;
        for (File file : sdkFolder.listFiles()) {
            if (file.getName().indexOf(CORE_JAR_PREFIX) != -1 || file.getName().indexOf(DISTRIBUTION_JAR_PREFIX) != -1) {
                coreJarName = file.getAbsolutePath();
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.TRACE, "Found core jar: %s", coreJarName);
                break;
            }
        }

        if (coreJarName == null) {
            String errorMessage = String.format("Did not find core jar in path %s", path);
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, errorMessage);
            throw new Exception(errorMessage);
        }

        InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.TRACE, "Found jar: " + coreJarName);

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(coreJarName);
        } catch (IOException e) {
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Could not load jar: " + coreJarName);
            throw e;
        }
        Enumeration<JarEntry> e = jarFile.entries();

        URL[] urls = { new URL("jar:file:" + coreJarName+"!/") };
        URLClassLoader cl = URLClassLoader.newInstance(urls);

        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if(je.isDirectory() || !je.getName().endsWith(".class")){
                continue;
            }
            try {
                Class clazz = cl.loadClass(CORE_SELF_REGISTRATOR_CLASS_NAME);
                clazz.getDeclaredConstructor().newInstance();
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.TRACE, "Loaded core jar");
                break;
            } catch (ClassNotFoundException e1) {
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Could not load class: %s, ClassNotFoundException", CORE_SELF_SHORT_REGISTRATOR_CLASS_NAME);
                throw e1;
            } catch (InvocationTargetException e1) {
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Could not load class: %s, InvocationTargetException", CORE_SELF_SHORT_REGISTRATOR_CLASS_NAME);
                throw e1;
            } catch (NoSuchMethodException e1) {
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Could not load class: %s, NoSuchMethodException", CORE_SELF_SHORT_REGISTRATOR_CLASS_NAME);
                throw e1;
            } catch (InstantiationException e1) {
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Could not load class: %s, InstantiationException", CORE_SELF_SHORT_REGISTRATOR_CLASS_NAME);
                throw e1;
            } catch (IllegalAccessException e1) {
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Could not load class: %s, IllegalAccessException", CORE_SELF_SHORT_REGISTRATOR_CLASS_NAME);
                throw e1;
            }
        }
    }
}
