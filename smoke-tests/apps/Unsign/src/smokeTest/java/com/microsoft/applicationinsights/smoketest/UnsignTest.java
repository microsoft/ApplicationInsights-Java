// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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
