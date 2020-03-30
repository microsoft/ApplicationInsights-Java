/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.liberty;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ThreadLocalContext {

  private static final ThreadLocal<ThreadLocalContext> local = new ThreadLocal<>();

  private final HttpServletRequest req;
  private final HttpServletResponse res;
  private Context context;
  private Scope scope;
  private boolean started;

  private ThreadLocalContext(HttpServletRequest req, HttpServletResponse res) {
    this.req = req;
    this.res = res;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public HttpServletRequest getRequest() {
    return req;
  }

  public HttpServletResponse getResponse() {
    return res;
  }

  /**
   * Test whether span should be started.
   *
   * @return true when span should be started, false when span was already started
   */
  public boolean startSpan() {
    boolean b = started;
    started = true;
    return !b;
  }

  public static void startRequest(HttpServletRequest req, HttpServletResponse res) {
    ThreadLocalContext ctx = new ThreadLocalContext(req, res);
    local.set(ctx);
  }

  public static ThreadLocalContext get() {
    return local.get();
  }

  public static ThreadLocalContext endRequest() {
    ThreadLocalContext ctx = local.get();
    if (ctx != null) {
      local.remove();
    }
    return ctx;
  }
}
