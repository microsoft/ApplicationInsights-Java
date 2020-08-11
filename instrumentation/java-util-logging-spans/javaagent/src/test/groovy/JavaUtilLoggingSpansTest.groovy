/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.log.LogEventsTestBase
import java.util.logging.Logger

class JavaUtilLoggingSpansTest extends LogEventsTestBase {

  @Override
  Object createLogger(String name) {
    Logger.getLogger(name)
  }

  String warn() {
    return "warning"
  }

  String error() {
    return "severe"
  }
}
