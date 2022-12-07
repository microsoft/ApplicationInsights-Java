// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import org.slf4j.Logger;

class AppInsightsCertificate {

  // DigiCert Global Root G2
  private static final String APP_INSIGHT_ROOT_CERTIFICATE =
      "-----BEGIN CERTIFICATE-----"
          + System.lineSeparator()
          + "MIIDjjCCAnagAwIBAgIQAzrx5qcRqaC7KGSxHQn65TANBgkqhkiG9w0BAQsFADBh"
          + System.lineSeparator()
          + "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3"
          + System.lineSeparator()
          + "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBH"
          + System.lineSeparator()
          + "MjAeFw0xMzA4MDExMjAwMDBaFw0zODAxMTUxMjAwMDBaMGExCzAJBgNVBAYTAlVT"
          + System.lineSeparator()
          + "MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j"
          + System.lineSeparator()
          + "b20xIDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IEcyMIIBIjANBgkqhkiG"
          + System.lineSeparator()
          + "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuzfNNNx7a8myaJCtSnX/RrohCgiN9RlUyfuI"
          + System.lineSeparator()
          + "2/Ou8jqJkTx65qsGGmvPrC3oXgkkRLpimn7Wo6h+4FR1IAWsULecYxpsMNzaHxmx"
          + System.lineSeparator()
          + "1x7e/dfgy5SDN67sH0NO3Xss0r0upS/kqbitOtSZpLYl6ZtrAGCSYP9PIUkY92eQ"
          + System.lineSeparator()
          + "q2EGnI/yuum06ZIya7XzV+hdG82MHauVBJVJ8zUtluNJbd134/tJS7SsVQepj5Wz"
          + System.lineSeparator()
          + "tCO7TG1F8PapspUwtP1MVYwnSlcUfIKdzXOS0xZKBgyMUNGPHgm+F6HmIcr9g+UQ"
          + System.lineSeparator()
          + "vIOlCsRnKPZzFBQ9RnbDhxSJITRNrw9FDKZJobq7nMWxM4MphQIDAQABo0IwQDAP"
          + System.lineSeparator()
          + "BgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBhjAdBgNVHQ4EFgQUTiJUIBiV"
          + System.lineSeparator()
          + "5uNu5g/6+rkS7QYXjzkwDQYJKoZIhvcNAQELBQADggEBAGBnKJRvDkhj6zHd6mcY"
          + System.lineSeparator()
          + "1Yl9PMWLSn/pvtsrF9+wX3N3KjITOYFnQoQj8kVnNeyIv/iPsGEMNKSuIEyExtv4"
          + System.lineSeparator()
          + "NeF22d+mQrvHRAiGfzZ0JFrabA0UWTW98kndth/Jsw1HKj2ZL7tcu7XUIOGZX1NG"
          + System.lineSeparator()
          + "Fdtom/DzMNU+MeKNhJ7jitralj41E6Vf8PlwUHBHQRFXGU7Aj64GxJUTFy8bJZ91"
          + System.lineSeparator()
          + "8rGOmaFvE7FBcf6IKshPECBV1/MUReXgRPTqh5Uykw7+U0b6LJ3/iyK5S9kJRaTe"
          + System.lineSeparator()
          + "pLiaWN0bfVKfjllDiIGknibVb63dDcY3fe0Dkhvld1927jyNxF1WW6LZZm6zNTfl"
          + System.lineSeparator()
          + "MrY="
          + System.lineSeparator()
          + "-----END CERTIFICATE-----"
          + System.lineSeparator();

  private final Logger startupLogger;

  AppInsightsCertificate(Logger startupLogger) {
    this.startupLogger = startupLogger;
  }

  boolean isInJavaKeystore() {
    String loadedCertificates = loadCertificates();
    return loadedCertificates.contains(APP_INSIGHT_ROOT_CERTIFICATE);
  }

  @SuppressFBWarnings(
      value = "SECCI", // Command Injection
      justification = "No user data is used to construct the command below")
  private String loadCertificates() {
    String keyStoreLocation = System.getProperty("java.home") + "/lib/security/cacerts";
    return executeWithoutException(
        new ProcessBuilder("keytool", "-list", "-rfc", "-keystore", keyStoreLocation));
  }

  private String executeWithoutException(ProcessBuilder processBuilder) {

    try {
      // to make sure the javaagent is not re-applied to the keytool process, which then creates
      // a recursive hanging
      // also helps to ensure other JVM args are not passed along
      processBuilder.environment().put("JAVA_TOOL_OPTIONS", "");
      processBuilder.environment().put("_JAVA_OPTIONS", "");

      Process process = processBuilder.start();
      OutputStream outputStream = process.getOutputStream();
      PrintWriter writer =
          new PrintWriter(new OutputStreamWriter(outputStream, Charset.defaultCharset()));
      writer.write(System.lineSeparator());
      writer.flush();
      writer.close();

      InputStream inputStream = process.getInputStream();
      String result = toString(inputStream);

      process.destroy();
      return result;
    } catch (Exception e) {
      startupLogger.error("Error related to the execution of " + processBuilder.command(), e);
      return "";
    }
  }

  private static String toString(InputStream inputStream) throws IOException {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
      return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
  }
}
