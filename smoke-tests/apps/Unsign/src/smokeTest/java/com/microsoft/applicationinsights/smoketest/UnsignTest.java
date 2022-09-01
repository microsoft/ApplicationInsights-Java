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

package com.microsoft.applicationinsights.smoketest;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

class UnsignTest {

  private static final File javaagentFile =
      new File(System.getProperty("ai.smoke-test.javaagent-file"));

  @Test
  void test() throws Exception {
    GenericContainer<?> container =
        new GenericContainer<>("openjdk:8")
            .withCopyFileToContainer(
                MountableFile.forHostPath(javaagentFile.toPath()), "/applicationinsights-agent.jar")
            .withCommand("sleep 60"); // so it won't terminate right away

    container.start();

    System.out.println(container.execInContainer("java", "-version"));
    System.out.println(
        container.execInContainer(
            "keytool",
            "-genkey",
            "-noprompt",
            "-alias",
            "alias",
            "-dname",
            "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown",
            "-keystore",
            "keystore",
            "-storetype",
            "pkcs12",
            "-storepass",
            "password",
            "-keypass",
            "password"));
    System.out.println(
        container.execInContainer(
            "jarsigner",
            "-keystore",
            "keystore",
            "applicationinsights-agent.jar",
            "-storepass",
            "password",
            "alias"));
    System.out.println(
        container.execInContainer("java", "-javaagent:applicationinsights-agent.jar", "-version"));
  }
}
