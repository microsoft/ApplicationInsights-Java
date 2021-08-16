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

package com.microsoft.applicationinsights.agent.internal.init;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this is temporary until OTel SDK 1.6.0 is released, which introduces ReadableSpan.getAttribute()
@SuppressWarnings("SystemOut")
class TempGetAttribute {

  private static final Logger logger = LoggerFactory.getLogger(TempGetAttribute.class);

  private static final MethodHandle lockMethodHandle;
  private static final MethodHandle attributesMapMethodHandle;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();

      Class<?> recordEventsReadableSpanClass =
          Class.forName("io.opentelemetry.sdk.trace.RecordEventsReadableSpan");

      Field lockField = recordEventsReadableSpanClass.getDeclaredField("lock");
      lockField.setAccessible(true);
      Field attributesField = recordEventsReadableSpanClass.getDeclaredField("attributes");
      attributesField.setAccessible(true);

      lockMethodHandle = lookup.unreflectGetter(lockField);
      attributesMapMethodHandle = lookup.unreflectGetter(attributesField);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  static <T> T getAttribute(ReadableSpan span, AttributeKey<T> key) {
    try {
      Object lock = lockMethodHandle.invoke(span);
      synchronized (lock) {
        Attributes attributes = (Attributes) attributesMapMethodHandle.invoke(span);
        return attributes.get(key);
      }
    } catch (Throwable e) {
      logger.debug(e.getMessage(), e);
      return null;
    }
  }

  private TempGetAttribute() {}
}
