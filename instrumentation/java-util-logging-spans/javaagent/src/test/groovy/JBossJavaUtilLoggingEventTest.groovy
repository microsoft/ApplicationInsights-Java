/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.log.LogEventsTestBase
import org.jboss.logmanager.LogContext

class JBossJavaUtilLoggingEventTest extends LogEventsTestBase {

  @Override
  Object createLogger(String name) {
    LogContext.create().getLogger(name)
  }

  String warn() {
    return "warning"
  }

  String error() {
    return "severe"
  }
}
