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
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.jar.JarFile;

import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

/**
 * Created by gupele on 5/6/2015.
 */
public final class AgentImplementation {

    private final static String AGENT_JAR_PREFIX = "applicationinsights-agent";

    private static String agentJarLocation;

    public static void premain(String args, Instrumentation inst) {

        try {
            agentJarLocation = getAgentJarLocation();
            appendJarsToBootstrapClassLoader(inst);
            loadJarsToBootstrapClassLoader(inst);
        } catch (Throwable throwable) {
            InternalAgentLogger.INSTANCE.error("Agent is NOT activated: failed to load to bootstrap class loader: " + throwable.getMessage());
            System.exit(-1);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadJarsToBootstrapClassLoader(Instrumentation inst) throws Throwable {
        ClassLoader bcl = AgentImplementation.class.getClassLoader().getParent();
        Class<CodeInjector> cic = (Class<CodeInjector>) bcl.loadClass("com.microsoft.applicationinsights.agent.internal.agent.CodeInjector");
        if (cic == null) {
            throw new IllegalStateException("Failed to load CodeInjector");
        }

        cic.getDeclaredConstructor(Instrumentation.class, String.class).newInstance(inst, agentJarLocation);
    }

    private static void appendJarsToBootstrapClassLoader(Instrumentation inst) throws Throwable {
        String agentJarPath = agentJarLocation.startsWith("file:/") ? agentJarLocation : "file:/" + agentJarLocation;

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

        InternalAgentLogger.INSTANCE.trace("Successfully loaded Agent jar");
    }

    public static String getAgentJarLocation() throws UnsupportedEncodingException {
        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if ((systemClassLoader instanceof URLClassLoader)) {
                for (URL url : ((URLClassLoader)systemClassLoader).getURLs()) {
                    String urlPath = url.getPath();
                    if (urlPath.indexOf(AGENT_JAR_PREFIX) != -1) {
                        int index = urlPath.lastIndexOf('/');
                        urlPath = urlPath.substring(0, index + 1);
                        return urlPath;
                    }
                }
            }
        } catch (Throwable throwable) {
            InternalAgentLogger.INSTANCE.error("Error while trying to fetch Jar Location, Exception: " + throwable.getMessage());
        }

        String path = AgentImplementation.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        return URLDecoder.decode(path, "UTF-8");
    }
}
