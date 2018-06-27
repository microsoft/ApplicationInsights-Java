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

package com.microsoft.applicationinsights.internal.channel.common;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;

/** Created by gupele on 6/4/2015. */
public enum ApacheSenderFactory {
  INSTANCE;

  private ApacheSender apacheSender;

  public synchronized ApacheSender create() {
    if (apacheSender != null) {
      return apacheSender;
    }

    if (!ClassDataUtils.INSTANCE.verifyClassExists(
        "org.apache.http.conn.HttpClientConnectionManager")) {

      InternalLogger.INSTANCE.warn(
          "Found an old version of HttpClient jar, for best performance consider upgrading to version 4.3+");

      apacheSender = new ApacheSender42();
    } else {
      InternalLogger.INSTANCE.trace("Using Http Client version 4.3+");
      apacheSender = new ApacheSender43();
    }
    return apacheSender;
  }
}
