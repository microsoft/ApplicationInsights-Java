package com.microsoft.applicationinsights.agent.internal;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.jar.JarFile;

/**
 * Premain-Class in Manifest
 */
public class Premain {

    private Premain() {
    }

    public static void premain(@SuppressWarnings("unused") String agentArgs, Instrumentation instrumentation) {
        try {
            CodeSource cs = Premain.class.getProtectionDomain().getCodeSource();
            File agentJarFile = getAgentJarFile(cs);

            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(agentJarFile));

            Class<?> mainEntryPointClass = Class.forName(
                    "com.microsoft.applicationinsights.agent.internal.MainEntryPoint", true,
                    Premain.class.getClassLoader());
            Method premainMethod = mainEntryPointClass.getMethod("premain", Instrumentation.class, File.class);
            premainMethod.invoke(null, instrumentation, agentJarFile);

        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            System.err.println("AgentImplementation3 failed to start: " + t.getLocalizedMessage());
            t.printStackTrace();
        }
    }

    static File getAgentJarFile(CodeSource cs) throws IOException, URISyntaxException {
        if (cs == null) {
            throw new IOException("Could not determine agent jar location.");
        }
        File csfile = new File(cs.getLocation().toURI());
        if (csfile.getName().endsWith(".jar")) {
            return csfile;
        }
        throw new IOException("Could not find agent jar. Found: " + csfile.getName());
    }
}
