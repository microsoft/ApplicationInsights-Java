// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

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
