/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.auto.micrometer;

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
  public DaemonThreadFactory(final String threadName) {
    this.threadName = threadName;
  }

  @Override
  public Thread newThread(final Runnable r) {
    final Thread thread = new Thread(r, threadName);
    thread.setDaemon(true);
    thread.setContextClassLoader(null);
    return thread;
  }
}
