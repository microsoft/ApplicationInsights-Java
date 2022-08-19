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

package io.opentelemetry.instrumentation.api.instrumenter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public final class Utils {

  public static final String IS_SYNTHETIC = "isSynthetic";
  public static final String TARGET = "target";
  public static final String IS_PRE_AGGREGATED = "isPreAggregated";

  @SuppressWarnings("SystemOut")
  public static boolean isUserAgentBot(Attributes endAttributes, Attributes startAttributes) {
    String aiUserAgent =
        getAttribute(AttributeKey.stringKey("ai.user.userAgent"), endAttributes, startAttributes);
    // TODO to be removed debug log
    System.out.println("############## HttpServerMetrics::aiUserAgent: " + aiUserAgent);
    if (aiUserAgent != null && aiUserAgent.indexOf("AlwaysOn") >= 0) {
      return true;
    }
    return false;
  }

  @Nullable
  private static <T> T getAttribute(AttributeKey<T> key, Attributes... attributesList) {
    for (Attributes attributes : attributesList) {
      T value = attributes.get(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private Utils() {}
}
