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

package com.microsoft.applicationinsights.agent.internal.common;

import com.microsoft.applicationinsights.agent.internal.configuration.DefaultEndpoints;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public class NetworkFriendlyExceptions {

  // returns true if the exception was "handled" and the caller should not log it
  public static boolean logSpecialOneTimeFriendlyException(
      Throwable error, String url, AtomicBoolean alreadySeen, Logger logger) {
    // Handle SSL cert exceptions
    SSLHandshakeException sslException = getCausedByOfType(error, SSLHandshakeException.class);
    if (sslException != null) {
      if (!alreadySeen.getAndSet(true)) {
        logger.error(getSslFriendlyMessage(url));
      }
      return true;
    }
    UnknownHostException unknownHostException =
        getCausedByOfType(error, UnknownHostException.class);
    if (unknownHostException != null && !alreadySeen.getAndSet(true)) {
      // TODO log friendly message with instructions how to troubleshoot
      //  e.g. wrong host address or cannot reach address due to network issues...
      return false;
    }

    IOException ioException = getCausedByOfType(error, IOException.class);
    if (ioException != null) {
      if (!alreadySeen.getAndSet(true)) {
        List<String> missingCiphers;
        try {
          missingCiphers = getMissingCiphers();
        } catch (NoSuchAlgorithmException e) {
          logger.error(e.getMessage(), e);
          return false;
        }
        if (missingCiphers != null && missingCiphers.size() > 0) {
          logger.error(getCipherFriendlyMessage(url, missingCiphers));
        } else {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private static List<String> getMissingCiphers() throws NoSuchAlgorithmException {
    final List<String> missingCiphers = new ArrayList<>();
    final List<String> expectedCiphers =
        Arrays.asList(
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256");
    SSLSocketFactory socketFactory = SSLContext.getDefault().getSocketFactory();
    List<String> cipherSuitesFromJvm = Arrays.asList(socketFactory.getSupportedCipherSuites());
    for (String cipher : expectedCiphers) {
      if (!cipherSuitesFromJvm.contains(cipher)) {
        missingCiphers.add(cipher);
      }
    }
    return missingCiphers;
  }

  private static <T extends Exception> T getCausedByOfType(Throwable throwable, Class<T> type) {
    if (type.isInstance(throwable)) {
      @SuppressWarnings("unchecked")
      T ofType = (T) throwable;
      return ofType;
    }
    Throwable cause = throwable.getCause();
    if (cause == null) {
      return null;
    }
    return getCausedByOfType(cause, type);
  }

  private static String getSslFriendlyMessage(String url) {
    return FriendlyException.populateFriendlyMessage(
        "Unable to find valid certification path to requested target.",
        getSslFriendlyExceptionAction(url),
        getFriendlyExceptionBanner(url),
        "This message is only logged the first time it occurs after startup.");
  }

  private static String getCipherFriendlyMessage(String url, List<String> missingCiphers) {
    return FriendlyException.populateFriendlyMessage(
        "Probable root cause may be : missing cipher suites which are expected by the requested target.",
        getCipherFriendlyExceptionAction(url, missingCiphers),
        getFriendlyExceptionBanner(url),
        "This message is only logged the first time it occurs after startup.");
  }

  private static String getFriendlyExceptionBanner(String url) {
    if (url.contains(DefaultEndpoints.LIVE_ENDPOINT)) {
      return "ApplicationInsights Java Agent failed to connect to Live metric end point.";
    }
    return "ApplicationInsights Java Agent failed to send telemetry data.";
  }

  private static String getCipherFriendlyExceptionAction(String url, List<String> missingCiphers) {
    StringBuilder actionBuilder = new StringBuilder();
    actionBuilder
        .append("The following are the cipher suites missing from Java runtime: ")
        .append("\n");
    for (String missingCipher : missingCiphers) {
      actionBuilder.append(missingCipher).append("\n");
    }
    actionBuilder
        .append(
            "Please add at least one of the missing cipher suites that are expected from the target endpoint:"
                + url)
        .append("\n");
    actionBuilder.append(
        "Learn more about troubleshooting this network issue related to cipher suites here: https://go.microsoft.com/fwlink/?linkid=2185426");
    return actionBuilder.toString();
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

  private NetworkFriendlyExceptions() {}
}
