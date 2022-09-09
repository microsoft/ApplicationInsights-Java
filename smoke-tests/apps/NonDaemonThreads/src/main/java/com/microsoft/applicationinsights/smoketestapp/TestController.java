// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  private static final Logger logger = LoggerFactory.getLogger(TestController.class);

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/spawn-another-java-process")
  public String spawnAnotherJavaProcess() throws Exception {
    Class<?> clazz =
        Class.forName(
            "org.springframework.boot.loader.JarLauncher",
            false,
            ClassLoader.getSystemClassLoader());
    File appJar = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
    File agentJar = getAgentJarFile();
    List<String> command =
        Arrays.asList(
            "java",
            "-javaagent:" + agentJar.getAbsolutePath(),
            "-jar",
            appJar.toString(),
            "okhttp3");
    ProcessBuilder builder = new ProcessBuilder(command);
    Process process = builder.inheritIO().start();
    process.waitFor();
    return "OK!";
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
