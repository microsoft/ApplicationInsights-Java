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

package com.microsoft.applicationinsights.agent.internal;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.jar.JarFile;

/**
 * Premain-Class in Manifest.
 */
public class Premain {

    private Premain() {
    }

    public static void premain(@SuppressWarnings("unused") String agentArgs, Instrumentation instrumentation) {
        try {
            CodeSource codeSource = Premain.class.getProtectionDomain().getCodeSource();
            File agentJarFile = getAgentJarFile(codeSource);

            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(agentJarFile));

            Class<?> mainEntryPointClass = Class.forName(
                    "com.microsoft.applicationinsights.agent.internal.MainEntryPoint", true,
                    Premain.class.getClassLoader());
            Method premainMethod = mainEntryPointClass.getMethod("premain", Instrumentation.class, File.class);
            premainMethod.invoke(null, instrumentation, agentJarFile);

        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            System.err.println("Agent failed to start: " + t.getLocalizedMessage());
            t.printStackTrace();
        }
    }

    private static File getAgentJarFile(CodeSource codeSource) throws IOException, URISyntaxException {
        if (codeSource == null) {
            throw new IOException("Could not determine agent jar location.");
        }
        File file = new File(codeSource.getLocation().toURI());
        if (file.getName().endsWith(".jar")) {
            return file;
        }
        throw new IOException("Could not find agent jar, found: " + file.getAbsolutePath());
    }
}
