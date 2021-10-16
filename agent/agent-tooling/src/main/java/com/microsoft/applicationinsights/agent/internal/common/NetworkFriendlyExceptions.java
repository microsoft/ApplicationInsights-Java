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
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLHandshakeException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public class NetworkFriendlyExceptions {

  public static void logSpecialOneTimeFriendlyException(
      Throwable error, String url, AtomicBoolean alreadySeen, Logger logger) {
    if (alreadySeen.get()) {
      return;
    }
    // Handle SSL cert exceptions
    SSLHandshakeException sslException = getCausedByOfType(error, SSLHandshakeException.class);
    if (sslException != null && alreadySeen.getAndSet(true)) {
      logger.error(getSslFriendlyMessage(url));
      return;
    }
    UnknownHostException unknownHostException =
        getCausedByOfType(error, UnknownHostException.class);
    if (unknownHostException != null && alreadySeen.getAndSet(true)) {
      // TODO log friendly message with instructions how to troubleshoot
      //  e.g. wrong host address or cannot reach address due to network issues...
      return;
    }
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
        getSslFriendlyExceptionBanner(url),
        getSslFriendlyExceptionAction(url),
        "Unable to find valid certification path to requested target.",
        "This message is only logged the first time it occurs after startup.");
  }

  private static String getSslFriendlyExceptionBanner(String url) {
    if (url.equals(DefaultEndpoints.LIVE_ENDPOINT)) {
      return "ApplicationInsights Java Agent failed to connect to Live metric end point.";
    }
    return "ApplicationInsights Java Agent failed to send telemetry data.";
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
