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

package com.microsoft.applicationinsights.smoketestapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
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
    Process process = builder.start();
    ConsoleOutputPipe consoleOutputPipe =
        new ConsoleOutputPipe(process.getInputStream(), System.out);
    Executors.newSingleThreadExecutor().submit(consoleOutputPipe);
    ConsoleOutputPipe consoleErrorPipe =
        new ConsoleOutputPipe(process.getErrorStream(), System.err);
    Executors.newSingleThreadExecutor().submit(consoleErrorPipe);
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

  private static class ConsoleOutputPipe implements Runnable {

    private final InputStream in;
    private final OutputStream out;

    private ConsoleOutputPipe(InputStream in, OutputStream out) {
      this.in = in;
      this.out = out;
    }

    @Override
    public void run() {
      byte[] buffer = new byte[100];
      try {
        while (true) {
          int n = in.read(buffer);
          if (n == -1) {
            break;
          }
          out.write(buffer, 0, n);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
