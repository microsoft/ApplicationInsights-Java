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

package com.microsoft.applicationinsights.agent.internal.httpclient;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.DefaultEndpoints;
import java.io.File;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SslUtil {

  public static String friendlyMessage(String url) {
    return FriendlyException.populateFriendlyMessage(
        getSslFriendlyExceptionBanner(url),
        getSslFriendlyExceptionAction(url),
        getSslFriendlyExceptionMessage(),
        getSslFriendlyExceptionNote());
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

  private static String getSslFriendlyExceptionBanner(String url) {
    if (url.equals(DefaultEndpoints.LIVE_ENDPOINT)) {
      return "ApplicationInsights Java Agent failed to connect to Live metric end point.";
    }
    return "ApplicationInsights Java Agent failed to send telemetry data.";
  }

  private static String getSslFriendlyExceptionMessage() {
    return "Unable to find valid certification path to requested target.";
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

  private static String getSslFriendlyExceptionNote() {
    return "This message is only logged the first time it occurs after startup.";
  }

  private SslUtil() {}
}
