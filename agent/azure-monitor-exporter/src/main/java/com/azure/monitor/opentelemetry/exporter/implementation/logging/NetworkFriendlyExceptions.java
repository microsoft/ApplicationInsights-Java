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

package com.azure.monitor.opentelemetry.exporter.implementation.logging;

import com.azure.core.util.CoreUtils;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMessageIdConstants;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class NetworkFriendlyExceptions {

  private static final List<FriendlyExceptionDetector> DETECTORS;
  private static final Logger logger = LoggerFactory.getLogger(NetworkFriendlyExceptions.class);

  static {
    DETECTORS = new ArrayList<>();
    // Note this order is important to determine the right exception!
    // For example SSLHandshakeException extends IOException
    DETECTORS.add(SslExceptionDetector.create());
    DETECTORS.add(UnknownHostExceptionDetector.create());
    try {
      DETECTORS.add(CipherExceptionDetector.create());
    } catch (NoSuchAlgorithmException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  // returns true if the exception was "handled" and the caller should not log it
  public static boolean logSpecialOneTimeFriendlyException(
      Throwable error, String url, AtomicBoolean alreadySeen, Logger logger) {
    return logSpecialOneTimeFriendlyException(error, url, alreadySeen, logger, DETECTORS);
  }

  public static boolean logSpecialOneTimeFriendlyException(
      Throwable error,
      String url,
      AtomicBoolean alreadySeen,
      Logger logger,
      List<FriendlyExceptionDetector> detectors) {

    for (FriendlyExceptionDetector detector : detectors) {
      if (detector.detect(error)) {
        if (!alreadySeen.getAndSet(true)) {
          MDC.put(
              AzureMonitorMessageIdConstants.MDC_MESSAGE_ID,
              String.valueOf(AzureMonitorMessageIdConstants.NETWORK_FAILURE_ERROR));
          logger.error(detector.message(url));
          MDC.remove(AzureMonitorMessageIdConstants.MDC_MESSAGE_ID);
        }
        return true;
      }
    }
    return false;
  }

  private static boolean hasCausedByWithMessage(Throwable throwable, String message) {
    if (throwable.getMessage().contains(message)) {
      return true;
    }
    Throwable cause = throwable.getCause();
    if (cause == null) {
      return false;
    }
    return hasCausedByWithMessage(cause, message);
  }

  private static boolean hasCausedByOfType(Throwable throwable, Class<?> type) {
    if (type.isInstance(throwable)) {
      return true;
    }
    Throwable cause = throwable.getCause();
    if (cause == null) {
      return false;
    }
    return hasCausedByOfType(cause, type);
  }

  private static String getFriendlyExceptionBanner(String url) {
    return "ApplicationInsights Java Agent failed to connect to " + url;
  }

  private static String populateFriendlyMessage(
      String description, String action, String banner, String note) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(System.lineSeparator());
    messageBuilder.append("*************************").append(System.lineSeparator());
    messageBuilder.append(banner).append(System.lineSeparator());
    messageBuilder.append("*************************").append(System.lineSeparator());
    if (!CoreUtils.isNullOrEmpty(description)) {
      messageBuilder.append(System.lineSeparator());
      messageBuilder.append("Description:").append(System.lineSeparator());
      messageBuilder.append(description).append(System.lineSeparator());
    }
    if (!CoreUtils.isNullOrEmpty(action)) {
      messageBuilder.append(System.lineSeparator());
      messageBuilder.append("Action:").append(System.lineSeparator());
      messageBuilder.append(action).append(System.lineSeparator());
    }
    if (!CoreUtils.isNullOrEmpty(note)) {
      messageBuilder.append(System.lineSeparator());
      messageBuilder.append("Note:").append(System.lineSeparator());
      messageBuilder.append(note).append(System.lineSeparator());
    }
    return messageBuilder.toString();
  }

  interface FriendlyExceptionDetector {
    boolean detect(Throwable error);

    String message(String url);
  }

  static class SslExceptionDetector implements FriendlyExceptionDetector {

    static SslExceptionDetector create() {
      return new SslExceptionDetector();
    }

    @Override
    public boolean detect(Throwable error) {
      if (error instanceof SslHandshakeTimeoutException) {
        return false;
      }
      // we are getting lots of SSLHandshakeExceptions in app services, and we suspect some may not
      // be certificate errors, so further restricting the condition to include the message
      return hasCausedByOfType(error, SSLHandshakeException.class)
          && hasCausedByWithMessage(
              error, "unable to find valid certification path to requested target");
    }

    @Override
    public String message(String url) {
      return populateFriendlyMessage(
          "Unable to find valid certification path to requested target.",
          getSslFriendlyExceptionAction(url),
          getFriendlyExceptionBanner(url),
          "This message is only logged the first time it occurs after startup.");
    }

    private static String getJavaCacertsPath() {
      String javaHome = System.getProperty("java.home");
      return new File(javaHome, "lib/security/cacerts").getPath();
    }

    @Nullable
    private static String getCustomJavaKeystorePath() {
      String cacertsPath = System.getProperty("javax.net.ssl.trustStore");
      if (cacertsPath != null) {
        return new File(cacertsPath).getPath();
      }
      return null;
    }

    private static String getSslFriendlyExceptionAction(String url) {
      String customJavaKeyStorePath = getCustomJavaKeystorePath();
      if (customJavaKeyStorePath != null) {
        return "Please import the SSL certificate from "
            + url
            + ", into your custom java key store located at:\n"
            + customJavaKeyStorePath
            + "\n"
            + "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
      }
      return "Please import the SSL certificate from "
          + url
          + ", into the default java key store located at:\n"
          + getJavaCacertsPath()
          + "\n"
          + "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
    }
  }

  static class UnknownHostExceptionDetector implements FriendlyExceptionDetector {

    static UnknownHostExceptionDetector create() {
      return new UnknownHostExceptionDetector();
    }

    @Override
    public boolean detect(Throwable error) {
      return hasCausedByOfType(error, UnknownHostException.class);
    }

    @Override
    public String message(String url) {
      return populateFriendlyMessage(
          "Unable to resolve host in url",
          getUnknownHostFriendlyExceptionAction(url),
          getFriendlyExceptionBanner(url),
          "This message is only logged the first time it occurs after startup.");
    }

    private static String getUnknownHostFriendlyExceptionAction(String url) {
      return "Please update your network configuration so that the host in this url can be resolved: "
          + url
          + "\nLearn more about troubleshooting unknown host exception here: https://go.microsoft.com/fwlink/?linkid=2185830";
    }
  }

  static class CipherExceptionDetector implements FriendlyExceptionDetector {

    private static final List<String> EXPECTED_CIPHERS =
        Arrays.asList(
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256");
    private final List<String> cipherSuitesFromJvm;

    static CipherExceptionDetector create() throws NoSuchAlgorithmException {
      SSLSocketFactory socketFactory = SSLContext.getDefault().getSocketFactory();
      return new CipherExceptionDetector(Arrays.asList(socketFactory.getSupportedCipherSuites()));
    }

    CipherExceptionDetector(List<String> cipherSuitesFromJvm) {
      this.cipherSuitesFromJvm = cipherSuitesFromJvm;
    }

    @Override
    public boolean detect(Throwable error) {
      if (!hasCausedByOfType(error, IOException.class)) {
        return false;
      }
      for (String cipher : EXPECTED_CIPHERS) {
        if (cipherSuitesFromJvm.contains(cipher)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String message(String url) {
      String description =
          "The JVM does not have any of the cipher suites which are supported by the endpoint \""
              + url
              + "\"";
      String enableEcc = System.getProperty("com.sun.net.ssl.enableECC");
      if ("false".equalsIgnoreCase(enableEcc)) {
        return populateFriendlyMessage(
            description
                + ", because the system property \"com.sun.net.ssl.enableECC\" is set"
                + " to \""
                + enableEcc
                + "\".",
            "Remove \"-Dcom.sun.net.ssl.enableECC=" + enableEcc + "\" from your command line.",
            getFriendlyExceptionBanner(url),
            "This message is only logged the first time it occurs after startup.");
      }
      return populateFriendlyMessage(
          description + ".",
          getCipherFriendlyExceptionAction(),
          getFriendlyExceptionBanner(url),
          "This message is only logged the first time it occurs after startup.");
    }

    private String getCipherFriendlyExceptionAction() {
      StringBuilder actionBuilder = new StringBuilder();
      actionBuilder
          .append(
              "Investigate why the security providers in your Java distribution's"
                  + " java.security configuration file differ from a standard Java distribution.")
          .append("\n\n");
      for (String missingCipher : EXPECTED_CIPHERS) {
        actionBuilder.append("    ").append(missingCipher).append("\n");
      }
      actionBuilder.append(
          "\nHere are the cipher suites that the JVM does have, in case this is"
              + " helpful in identifying why the ones above are missing:\n");
      for (String foundCipher : cipherSuitesFromJvm) {
        actionBuilder.append(foundCipher).append("\n");
      }
      // even though we log this info at startup, this info is particularly important for this error
      // so we duplicate it here to make sure we get it as quickly and as easily as possible
      actionBuilder.append(
          "\nJava version:"
              + System.getProperty("java.version")
              + ", vendor: "
              + System.getProperty("java.vendor")
              + ", home: "
              + System.getProperty("java.home"));
      actionBuilder.append(
          "\nLearn more about troubleshooting this network issue related to cipher suites here:"
              + " https://go.microsoft.com/fwlink/?linkid=2185426");
      return actionBuilder.toString();
    }
  }

  private NetworkFriendlyExceptions() {}
}
