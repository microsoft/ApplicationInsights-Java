package com.microsoft.ajl.simple;

import com.google.common.base.Joiner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@RestController
public class TestController {

    @GetMapping("/")
    public String root() {
        return "OK";
    }


    @GetMapping("/verifyShading")
    public String verifyShading() throws IOException {

        List<String> unexpectedEntries = getUnexpectedEntries();
        if (!unexpectedEntries.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String unexpectedEntry : unexpectedEntries) {
                sb.append(' ');
                sb.append(unexpectedEntry);
            }
            throw new AssertionError("Found unexpected entries in agent jar:" + sb);
        }

        return "OK";
    }

    public List<String> getUnexpectedEntries() throws IOException {
        File agentJarFile = getAgentJarFile();
        List<String> expectedEntries = new ArrayList<>();
        expectedEntries.add("com/");
        expectedEntries.add("com/microsoft/");
        expectedEntries.add("com/microsoft/applicationinsights/");
        expectedEntries.add("com/microsoft/applicationinsights/agentc/.*");
        expectedEntries.add("com/microsoft/applicationinsights/instrumentation/.*");
        expectedEntries.add("org/");
        expectedEntries.add("org/glowroot/");
        expectedEntries.add("org/glowroot/instrumentation/.*");
        expectedEntries.add("META-INF/");
        expectedEntries.add("META-INF/services/");
        expectedEntries.add("META-INF/services/com\\.microsoft\\.applicationinsights\\.agentc\\..*");
        expectedEntries.add("META-INF/instrumentation.list");
        expectedEntries.add("META-INF/MANIFEST\\.MF");
        expectedEntries.add("applicationinsights-core-native-win32.dll");
        expectedEntries.add("applicationinsights-core-native-win64.dll");
        expectedEntries.add("LICENSE");
        expectedEntries.add("NOTICE");
        expectedEntries.add("ai.logback.xml");
        expectedEntries.add("sdk-version.properties");
        JarFile jarFile = new JarFile(agentJarFile);
        List<String> unexpected = new ArrayList<>();
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
            JarEntry jarEntry = e.nextElement();
            if (!acceptableJarEntry(jarEntry, expectedEntries)) {
                unexpected.add(jarEntry.getName());
            }
        }
        jarFile.close();
        return unexpected;
    }

    private static boolean acceptableJarEntry(JarEntry jarEntry, List<String> acceptableEntries) {
        for (String acceptableEntry : acceptableEntries) {
            if (jarEntry.getName().matches(acceptableEntry)) {
                return true;
            }
        }
        return false;
    }

    private static File getAgentJarFile() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String jvmArg : jvmArgs) {
            if (jvmArg.startsWith("-javaagent:") && jvmArg.contains("applicationinsights-agent-codeless")) {
                return new File(jvmArg.substring("-javaagent:".length()));
            }
        }
        throw new AssertionError("Agent jar not found on command line: " + Joiner.on(' ').join(jvmArgs));
    }
}
