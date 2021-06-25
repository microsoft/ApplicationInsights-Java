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

package com.microsoft.applicationinsights.agent.internal.wascore.util;

import com.microsoft.applicationinsights.agent.internal.wascore.common.FriendlyException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLHandshakeException;
import org.slf4j.Logger;

public class ExceptionUtil {

  private static SSLHandshakeException getSslHandshakeException(Throwable t) {
    if (t instanceof SSLHandshakeException) {
      return (SSLHandshakeException) t;
    }
    Throwable cause = t.getCause();
    if (cause == null) {
      return null;
    }
    return getSslHandshakeException(cause);
  }

  private static Exception getSocketException(Throwable t) {
    if (t instanceof SocketException) {
      return (Exception) t;
    }
    Throwable cause = t.getCause();
    if (cause == null) {
      return null;
    }
    return getSocketException(cause);
  }

  public static void parseError(
      Throwable error, String url, AtomicBoolean exceptionThrown, Logger logger) {
    // Handle SSL cert exceptions
    SSLHandshakeException sslException = ExceptionUtil.getSslHandshakeException(error);
    if (sslException != null && !exceptionThrown.getAndSet(true)) {
      logger.error(
          FriendlyException.populateFriendlyMessage(
              SslUtil.getSslFriendlyExceptionBanner(url),
              SslUtil.getSslFriendlyExceptionAction(url),
              SslUtil.getSslFriendlyExceptionMessage(),
              SslUtil.getSslFriendlyExceptionNote()));
    }
    // TODO (kryalama) handle other network exceptions
  }

  private ExceptionUtil() {}
}
