// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    expectedEntries.add("com/microsoft/applicationinsights/agent/.*");
    expectedEntries.add("io/");
    expectedEntries.add("io/opentelemetry/");
    expectedEntries.add("io/opentelemetry/javaagent/.*");
    expectedEntries.add("META-INF/");
    expectedEntries.add("META-INF/maven/");
    expectedEntries.add("META-INF/maven/.*");
    expectedEntries.add("META-INF/licenses/");
    expectedEntries.add("META-INF/licenses/.*");
    expectedEntries.add("META-INF/LICENSE");
    expectedEntries.add("META-INF/MANIFEST\\.MF");
    expectedEntries.add("applicationinsights\\.appsvc\\.logback\\.xml");
    expectedEntries.add("applicationinsights\\.console\\.logback\\.xml");
    expectedEntries.add("applicationinsights\\.file\\.logback\\.xml");
    expectedEntries.add("applicationinsights\\.file-and-console\\.logback\\.xml");
    expectedEntries.add("ai\\.sdk-version\\.properties");
    expectedEntries.add("logger-config/");
    expectedEntries.add("logger-config/common\\.xml");
    expectedEntries.add("logger-config/console\\.appender\\.xml");
    expectedEntries.add("logger-config/file\\.appender\\.xml");
    expectedEntries.add("rp-logger-config/");
    expectedEntries.add("rp-logger-config/diagnostics\\.appender\\.xml");
    expectedEntries.add("rp-logger-config/etw\\.appender\\.xml");
    expectedEntries.add("applicationinsights-java-etw-provider-x86-64\\.dll");
    expectedEntries.add("applicationinsights-java-etw-provider-x86\\.dll");
    expectedEntries.add("inst/.*");
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
      if (jvmArg.startsWith("-javaagent:") && jvmArg.contains("applicationinsights-agent")) {
        return new File(jvmArg.substring("-javaagent:".length()));
      }
    }
    throw new AssertionError("Agent jar not found on command line: " + String.join(" ", jvmArgs));
  }
}
