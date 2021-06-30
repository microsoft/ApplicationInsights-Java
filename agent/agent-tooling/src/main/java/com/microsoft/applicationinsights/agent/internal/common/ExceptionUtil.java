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

import com.microsoft.applicationinsights.agent.internal.httpclient.SslUtil;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLHandshakeException;
import org.slf4j.Logger;

public class ExceptionUtil {

  public static void parseError(
      Throwable error, String url, AtomicBoolean exceptionThrown, Logger logger) {
    if (exceptionThrown.getAndSet(true)) {
      return;
    }
    // Handle SSL cert exceptions
    SSLHandshakeException sslException = getCausedByOfType(error, SSLHandshakeException.class);
    if (sslException != null) {
      logger.error(SslUtil.friendlyMessage(url));
      return;
    }
    SocketException socketException = getCausedByOfType(error, SocketException.class);
    if (socketException != null) {
      FriendlyException.getMessageWithDefaultBanner("socket exception: " + error.getMessage());
      return;
    }
    SocketTimeoutException socketTimeoutException =
        getCausedByOfType(error, SocketTimeoutException.class);
    if (socketTimeoutException != null) {
      FriendlyException.getMessageWithDefaultBanner(
          "socket timeout exception: " + error.getMessage());
      return;
    }
    UnknownHostException unknownHostException =
        getCausedByOfType(error, UnknownHostException.class);
    if (unknownHostException != null) {
      FriendlyException.getMessageWithDefaultBanner(
          "wrong host address or cannot reach address due to network issues: "
              + error.getMessage());
      return;
    }
    IOException ioException = getCausedByOfType(error, IOException.class);
    if (ioException != null) {
      FriendlyException.getMessageWithDefaultBanner("I/O exception: " + error.getMessage());
      return;
    }
    ConnectException connectException = getCausedByOfType(error, ConnectException.class);
    if (connectException != null) {
      FriendlyException.getMessageWithDefaultBanner("I/O exception: " + error.getMessage());
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

  private ExceptionUtil() {}
}
