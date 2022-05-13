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

package io.opentelemetry.javaagent.instrumentation.micrometer;

import java.util.concurrent.ThreadFactory;

// this is a copy of io.opentelemetry.auto.common.exec.DaemonThreadFactory
// so that instrumentation doesn't need to use io.micrometer.core.instrument.util.NamedThreadFactory
// which wasn't introduced until micrometer 1.0.12
public final class DaemonThreadFactory implements ThreadFactory {

  private final String threadName;

  /**
   * Constructs a new {@code DaemonThreadFactory} with a null ContextClassLoader.
   *
   * @param threadName used to prefix all thread names.
   */
  public DaemonThreadFactory(String threadName) {
    this.threadName = threadName;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(r, threadName);
    thread.setDaemon(true);
    thread.setContextClassLoader(null);
    return thread;
  }
}
